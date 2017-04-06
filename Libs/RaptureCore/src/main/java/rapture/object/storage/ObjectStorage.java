/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.object.storage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.Kernel;
import rapture.object.Storable;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;

/**
 * This is to store and retrieve {@link Storable} objects in a repo. It should not be used directly, but will be used from code that's autogenerated for each
 * {@link Storable} defined in types.api
 */

public abstract class ObjectStorage {

    private static final Logger log = Logger.getLogger(ObjectStorage.class);

    private static String getLowLevelContent(RaptureURI storageLocation, StorableIndexInfo indexInfo) {
        String path = storageLocation.getDocPath();
        Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
        if (repository != null) {
            return repository.getDocument(path);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", storageLocation.getAuthority()));
            return null;
        }
    }

    /**
     * Read a document and return it, converting it into the corresponding object
     *
     * @param storageLocation A {@link RaptureURI} indicating the location where this document is stored
     * @param storableClass   The {@link Class} of the object that is stored at this location
     * @return
     */
    public static <T extends Storable> T read(final RaptureURI storageLocation, Class<T> storableClass, StorableInfo storableInfo, ObjectMapper mapper) {
        String content;
        if (storableInfo.isCacheable()) {
            Optional<String> contentOptional = getContentCache().getIfPresent(storageLocation);
            if (contentOptional == null) {
                content = getLowLevelContent(storageLocation, storableInfo.getIndexInfo());
                if (content != null || storableInfo.shouldCacheNulls()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Putting " + storageLocation + " in local cache");
                    }
                    getContentCache().put(storageLocation, Optional.fromNullable(content));
                }
            } else {
                if (contentOptional.isPresent()) {
                    content = contentOptional.get();
                } else {
                    content = null;
                }
            }
        } else {
            content = getLowLevelContent(storageLocation, storableInfo.getIndexInfo());
        }
        if (content == null) {
            return null;
        } else {
            try {
                return mapper.readValue(content, storableClass);
            } catch (IOException e) {
                throw RaptureExceptionFactory
                        .create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making " + storableClass.getName() + " object from json " + content, e);
            }
        }
    }

    public static void visitAll(String repoName, String storableClassPrefix, String filterPrefix, StorableInfo storableInfo,
            RepoVisitor visitor) {
        Repository repo = getRepo(repoName, storableInfo.getIndexInfo());
        if (storableClassPrefix.endsWith("/") && filterPrefix.startsWith("/")) filterPrefix=filterPrefix.substring(1);
        String prefix = storableClassPrefix + filterPrefix;
        if (repo != null) {
            repo.visitAll(prefix, null, visitor);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", repoName));
        }
    }

    public static TableQueryResult queryIndex(StorableInfo storableInfo, String repoName, String query) {
        Repository repo = getRepo(repoName, storableInfo.getIndexInfo());
        if (repo != null) {
            return repo.findIndex(query);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", repoName));
            return new TableQueryResult();
        }
    }

    public static List<RaptureFolderInfo> removeFolder(String repoName, String storableClassPrefix, StorableInfo storableInfo,
            String parentFolderPath) {
        Repository repo = getRepo(repoName, storableInfo.getIndexInfo());
        String prefix;
        if (parentFolderPath == null || parentFolderPath.isEmpty()) {
            // strip trailing slash, repo.getChildren doesn't like that
            prefix = storableClassPrefix.substring(0, storableClassPrefix.length());
        } else {
            if (parentFolderPath.startsWith("/")) {
                parentFolderPath = parentFolderPath.replaceAll("^/*", "");
            }
            prefix = storableClassPrefix + parentFolderPath;
        }

        prefix = prefix.replaceAll("/*$", "");

        List<RaptureFolderInfo> realRet = new ArrayList<>();
        if (repo != null) {
            List<RaptureFolderInfo> ret = repo.removeChildren(prefix, true);
            getContentCache().invalidateAll();
            // Need to remove the storableClassPrefix from each of these and add back a //
            if (ret != null) {
                for (RaptureFolderInfo r : ret) {
                    RaptureFolderInfo realR = new RaptureFolderInfo();
                    realR.setFolder(r.isFolder());
                    realR.setName("//" + r.getName().substring(storableClassPrefix.length()));
                    realRet.add(realR);
                }
            }
        } else {
            log.error(String.format("Error, could not find repo name [%s]", repoName));
        }
        return realRet;
    }

    public static List<RaptureFolderInfo> getChildren(String repoName, String storableClassPrefix, StorableInfo storableInfo,
            String parentFolderPath) {
        String prefix;
        if (parentFolderPath == null || parentFolderPath.isEmpty()) {
            // strip trailing slash, repo.getChildren doesn't like that
            prefix = storableClassPrefix.substring(0, storableClassPrefix.length());
        } else {
            if (parentFolderPath.startsWith("/")) {
                parentFolderPath = parentFolderPath.replaceAll("^/*", "");
            }
            prefix = storableClassPrefix + parentFolderPath;
        }

        prefix = prefix.replaceAll("/*$", "");

        Repository repo = getRepo(repoName, storableInfo.getIndexInfo());
        if (repo != null) {
            return repo.getChildren(prefix);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", repoName));
            return new ArrayList<>();
        }
    }

    public static <T extends Storable> List<T> readAll(final Class<T> klass, String repoName, String storableClassPrefix,
            final StorableInfo storableInfo, String filterPrefix, final ObjectMapper mapper) {
        final List<T> ret = new ArrayList<>();
        visitAll(repoName, storableClassPrefix, filterPrefix, storableInfo, new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    T storable;
                    try {
                        storable = ObjectStorage.read(content, klass, mapper);
                        ret.add(storable);
                    } catch (RaptureException e) {
                        log.error(String.format("Got error reading %s: %s", klass.getSimpleName(), ExceptionToString.format(e)));
                    }
                }
                return true;
            }
        });
        return ret;
    }

    public static <T extends Storable> List<T> filterAll(final Class<T> klass, String repoName, String storableClassPrefix,
            final StorableInfo storableInfo, String filterPrefix, final ObjectFilter<T> filter, final ObjectMapper mapper) {
        final List<T> ret = new ArrayList<>();
        visitAll(repoName, storableClassPrefix, filterPrefix, storableInfo, new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    T storable;
                    try {
                        storable = ObjectStorage.read(content, klass, mapper);
                        if (filter.shouldInclude(storable)) {
                            ret.add(storable);
                        }
                    } catch (RaptureException e) {
                        log.error(String.format("Got error reading %s: %s", klass.getSimpleName(), ExceptionToString.format(e)));
                    }
                }
                return true;
            }
        });

        return ret;
    }

    public static Repository getRepo(String repoName, StorableIndexInfo indexInfo) {
        Optional<Repository> repo = Kernel.INSTANCE.getStorableRepo(repoName, indexInfo);
        if (repo.isPresent()) {
            return repo.get();
        } else {
            return null;
        }
    }

    public static <T extends Storable> T read(JsonContent string, Class<T> storableClass, ObjectMapper mapper) {
        String json = string.getContent();
        try {
            return mapper.readValue(json, storableClass);
        } catch (IOException e) {
            throw RaptureExceptionFactory
                    .create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making " + storableClass.getName() + " object from json " + json, e);
        }
    }

    /**
     * Store a document into the repo, and add a comment
     *
     * @param storable The document that will be stored
     * @param user     The user doing the storing
     * @param comment
     */
    public static DocumentWithMeta write(Storable storable, String user, StorableInfo storableInfo, String comment, ObjectMapper mapper) {
        RaptureURI storageLocation = storable.getStorageLocation();
        if (log.isTraceEnabled()) log.trace("ObjectStorage.addDoc called. Storage location is " + storageLocation.toString() + " user " + user + " comment "
                + comment);
        Repository repository = getRepo(storageLocation.getAuthority(), storableInfo.getIndexInfo());
        String path = storageLocation.getDocPath();
        String jsonString;
        try {
            jsonString = mapper.writeValueAsString(storable);
        } catch (JsonProcessingException e) {
            throw RaptureExceptionFactory.create("Error converting storable to json", e);
        }
        if (repository != null) {
           DocumentWithMeta meta = repository.addDocument(path, jsonString, user, comment, false);
            if (meta != null && log.isTraceEnabled()) log.trace("addDocument returns version " + meta.getMetaData().getVersion());
            getContentCache().put(storable.getStorageLocation(), Optional.of(jsonString));
            return meta;
        } else {
            log.error(String.format("Error, could not find repo for storable of type [%s]", storable.getClass().getName()));
        }
        return null;
    }

    public static boolean delete(String user, RaptureURI storageLocation, StorableIndexInfo indexInfo) {
        Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
        getContentCache().invalidate(storageLocation);
        if (repository != null) {
            return repository.removeDocument(storageLocation.getDocPath(), user, "Drop Doc");
        } else {
            log.error(String.format("Error, could not find repo name [%s]", storageLocation.getAuthority()));
            return false;
        }
    }

    public static boolean delete(String user, RaptureURI storageLocation, StorableIndexInfo indexInfo, String comment) {
        Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
        getContentCache().invalidate(storageLocation);
        if (repository != null) {
            return repository.removeDocument(storageLocation.getDocPath(), user, comment);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", storageLocation.getAuthority()));
            return false;
        }
    }

    public static Optional<DocumentMetadata> getLatestMeta(RaptureURI storageLocation, StorableIndexInfo indexInfo) {
        Repository repo = getRepo(storageLocation.getAuthority(), indexInfo);
        if (repo != null) {
            if (repo.hasMetaContent()) {
                return Optional.fromNullable(repo.getMeta(storageLocation.getDocPath(), null));
            } else {
                return Optional.absent();
            }
        } else {
            log.error(String.format("Error, could not find repo name [%s]", storageLocation.getAuthority()));
            return Optional.absent();
        }
    }
    
    public static DocumentWithMeta getDocumentWithMeta(RaptureURI storageLocation, StorableIndexInfo indexInfo) {
        String path = storageLocation.getDocPath();
        Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
        if (repository != null) {
            return repository.getDocAndMeta(path, null);
        } else {
            log.error(String.format("Error, could not find repo name [%s]", storageLocation.getAuthority()));
            return null;
        }  	
    }

    private static Cache<RaptureURI, Optional<String>> getContentCache() {
        return Kernel.getObjectStorageCache();
    }

	public static DocumentWithMeta applyTag(RaptureURI storageLocation,StorableIndexInfo indexInfo,
			String user, String tagUri, String tagValue) {
	    String path = storageLocation.getDocPath();
	    Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
	    DocumentWithMeta dm = repository.addTagToDocument(user, path, tagUri, tagValue);
		return dm;
	}
	
	public static DocumentWithMeta applyTags(RaptureURI storageLocation,StorableIndexInfo indexInfo,
			String user, Map<String, String> tagMap) {
	    String path = storageLocation.getDocPath();
	    Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
	    return repository.addTagsToDocument(user, path, tagMap);
	}
	
	public static DocumentWithMeta removeTag(RaptureURI storageLocation,StorableIndexInfo indexInfo,
			String user, String tagUri) {
	    String path = storageLocation.getDocPath();
	    Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
	    return repository.removeTagFromDocument(user, path, tagUri);
	}
	
	public static DocumentWithMeta removeTags(RaptureURI storageLocation,StorableIndexInfo indexInfo,
			String user, List<String> tags) {
	    String path = storageLocation.getDocPath();
	    Repository repository = getRepo(storageLocation.getAuthority(), indexInfo);
	    return repository.removeTagsFromDocument(user, path, tags);
	}
}
