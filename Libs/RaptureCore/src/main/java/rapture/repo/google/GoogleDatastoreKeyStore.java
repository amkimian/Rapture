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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY kind, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.repo.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DateTimeValue;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.LatLngValue;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.RawValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.StoreKeyVisitor;

public class GoogleDatastoreKeyStore extends AbstractKeyStore implements KeyStore {
    private static final Logger log = Logger.getLogger(GoogleDatastoreKeyStore.class);
    static String id = "high-plating-157918"; // Make it a config value
    private Datastore datastore = null;
    private String kind;

    public GoogleDatastoreKeyStore() {
        // TODO id must be configurable - currently it is not
        datastore = DatastoreOptions.newBuilder().setProjectId(id).build().getService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#setRepoLockHandler(rapture.repo. RepoLockHandler)
     */
    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        super.setRepoLockHandler(repoLockHandler);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#delete(java.util.List)
     */
    @Override
    public boolean delete(List<String> keys) {
        return super.delete(keys);
    }

    /*
     * Dropping the keystore basically means dropping all the entities This is NOT optimal, but it's only really used for testing. TODO Find a better way.
     */
    @Override
    public boolean dropKeyStore() {
        List<Key> keys = new ArrayList<>();
        QueryResults<Key> result = datastore.run(Query.newKeyQueryBuilder().setKind(kind).build());
        // Batch this
        while (result.hasNext()) {
            Key peele = result.next();
            datastore.delete(peele);
            System.out.println("Deleted " + peele.getName() + " from " + peele.getKind() + "parent " + peele.getParent());
        }

        return super.dropKeyStore();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#getBatch(java.util.List)
     */
    @Override
    public List<String> getBatch(List<String> keys) {
        return super.getBatch(keys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#runNativeQueryWithLimitAndBounds(java.lang. String, java.util.List, int, int)
     */
    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return super.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#visit(java.lang.String, rapture.repo.RepoVisitor)
     */
    @Override
    public void visit(String folderPrefix, RepoVisitor iRepoVisitor) {
        super.visit(folderPrefix, iRepoVisitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#matches(java.lang.String, java.lang.String)
     */
    @Override
    public boolean matches(String key, String value) {
        return super.matches(key, value);
    }

    @Override
    public boolean containsKey(String key) {
        Key taskKey = datastore.newKeyFactory().setKind(kind).newKey(key);
        return (datastore.get(taskKey) != null);
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        throw new RaptNotSupportedException("Not yet supported");
    }

    Map<String, String> config;

    @Override
    public void setConfig(Map<String, String> config) {
        String k = StringUtils.stripToNull(config.get("kind"));
        System.err.println("DEBUG: Instance name = " + kind + " overwritten by config = " + k);
        if (k != null) kind = k; // config takes precedence over instance name for now
        if (kind == null) throw new RuntimeException("kind must be specified");
        this.config = config;
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        KeyStore ks = new GoogleDatastoreKeyStore();
        ks.setInstanceName(kind + relation);
        return ks;
    }

    /**
     * Is Key a RaptureURI?
     */
    @Override
    public boolean delete(String key) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(key);
        datastore.delete(entityKey);
        return true;
    }

    private void put(Map<String, Object> map, String name, Value<?> value) {
        switch (value.getType()) {
        case STRING:
            map.put(name, ((StringValue) value).get());
            break;
        case NULL:
            map.put(name, null);
            break;
        case ENTITY:
            Map<String, Object> map2 = new HashMap<>();
            FullEntity<?> fe = ((EntityValue) value).get();
            Set<String> names = fe.getNames();
            for (String nom : names) {
                put(map2, nom, fe.getValue(nom));
            }
            map.put(name, map2);
            break;
        case LIST:
            List<? extends Value<?>> list = ((ListValue) value).get();
            List<String> slist = new ArrayList<>();
            // TODO this may be an oversimplification
            for (Value<?> v : list) {
                if (v instanceof StringValue) slist.add(((StringValue) v).get());
            }
            map.put(name, slist);
            break;
        case KEY:
            map.put(name, ((KeyValue) value).get());
            break;
        case LONG:
            map.put(name, ((LongValue) value).get());
            break;
        case DOUBLE:
            map.put(name, ((DoubleValue) value).get());
            break;
        case BOOLEAN:
            map.put(name, ((BooleanValue) value).get());
            break;
        case DATE_TIME:
            map.put(name, ((DateTimeValue) value).get());
            break;
        case BLOB:
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(((BlobValue) value).get().asInputStream(), baos);
                map.put(name, baos.toString());
            } catch (IOException e) {
                log.error("Cannot read blob: " + e.getMessage());
            }
            break;
        case RAW_VALUE:
            map.put(name, ((RawValue) value).get());
            break;
        case LAT_LNG:
            map.put(name, ((LatLngValue) value).get());
            break;
        default:
            throw new RuntimeException("Can't yet");
        }
    }

    @Override
    public String get(String key) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(key);
        Entity entity = datastore.get(entityKey);
        Map<String, Object> map = new HashMap<>();
        if (entity != null) {
            for (String name : entity.getNames()) {
                Value<?> value = entity.getValue(name);
                if (value != null) {
                    put(map, name, value);
                }
            }
        }
        if (map.isEmpty()) return null;
        return JacksonUtil.jsonFromObject(map);
    }

    @Override
    public String getStoreId() {
        return id;
    }

    private Value<?> valerie(String key, Object val) {
        Value<?> valerie;

        if (val instanceof Map) {
            com.google.cloud.datastore.FullEntity.Builder<IncompleteKey> builder = Entity.newBuilder();

            Set<Entry> entries = ((Map) val).entrySet();
            for (Entry e : entries) {
                String kiki = e.getKey().toString();
                builder.set(kiki, valerie(kiki, e.getValue()));
            }
            valerie = new EntityValue(builder.build());
        } else if (val instanceof String) {
            String str = val.toString();
            valerie = StringValue.newBuilder(str).setExcludeFromIndexes(str.length() >= 1500).build();
        } else if ((val instanceof Double) || (val instanceof Float)) valerie = new DoubleValue((Double) val);
        else if (val instanceof BigDecimal) valerie = new StringValue(((BigDecimal) val).toPlainString());
        else if (val instanceof Number) valerie = new LongValue(((Number) val).longValue());
        else if (val instanceof Boolean) valerie = new BooleanValue((Boolean) val);
        else if (val instanceof List) {
            List<Value<?>> valist = new ArrayList<>();
            for (Object o : ((List) val)) {
                valist.add(valerie(null, o));
            }
            valerie = new ListValue(valist);
        }
        else {
            log.warn("Not sure about " + val.getClass());
            valerie = new BlobValue(Blob.copyFrom(val.toString().getBytes()));
        }
        return valerie;
    }

    /**
     * TODO https://cloud.google.com/datastore/docs/best-practices
     *
     * For a key that uses a custom name, always use UTF-8 characters except a forward slash (/). Non-UTF-8 characters interfere with various processes such as
     * importing a Cloud Datastore backup into Google BigQuery. A forward slash could interfere with future functionality.
     */
    @Override
    public void put(String key, String value) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(key);
        Map<String, Object> map = JacksonUtil.getMapFromJson(value);
        Builder builder = Entity.newBuilder(entityKey);
        for (Entry<String, Object> entry : map.entrySet()) {
            builder.set(entry.getKey(), valerie(entry.getKey(), entry.getValue()));
        }
        Entity entity = builder.build();

        try {
            datastore.put(entity);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("PUT KEY " + key + " VALUE " + value);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        if (repoType.toUpperCase().equals("GDS")) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "RepoType mismatch. Repo is of type GDS, asked for " + repoType);
        }
    }

    /**
     * What is the difference between visitKeysFromStart and visitKeys?
     */
    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        List<Key> keys = new ArrayList<>();
        QueryResults<Key> result = datastore.run(Query.newKeyQueryBuilder().setKind(kind).build());
        int count = 0;
        while (result.hasNext()) {
            Key peele = result.next();
            String jordan = peele.getName();
            count++;
            System.out.println("" + count + " : " + jordan);
            if (jordan.startsWith(prefix)) {
                String keegan = this.get(jordan);
                if ((keegan != null) && !iStoreKeyVisitor.visit(jordan, keegan)) {
                    break;
                }
            }
        }
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        visitKeys(startPoint, iStoreKeyVisitor);
    }

    @Override
    public void setInstanceName(String name) {
        kind = name;
    }

    public static final java.lang.String KEY_RESERVED_PROPERTY = "__key__";

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        // Must be a better way of doing this, but PropertyFilter.hasAncestor did not work.
        if ((StringUtils.stripToNull(prefix) != null) && !prefix.endsWith("/")) prefix = prefix + "/";

        List<RaptureFolderInfo> list = new ArrayList<>();
        Map<String, RaptureFolderInfo> map = new HashMap<>();

        KeyQuery.Builder query = Query.newKeyQueryBuilder().setKind(kind);
        // if (StringUtils.stripToNull(prefix) != null) {
        // query.setFilter(PropertyFilter.hasAncestor(datastore.newKeyFactory().setKind(kind).newKey(prefix)));
        // }
        QueryResults<Key> result = datastore.run(query.build());
        while (result.hasNext()) {
            Key peele = result.next();
            String jordan = peele.getName();
            if (jordan.startsWith(prefix)) {
                String keegan = jordan.substring(prefix.length());
                int idx = keegan.indexOf('/');
                if (idx > 0) {
                    list.add(new RaptureFolderInfo(keegan.substring(0, idx), true));
                } else {
                    list.add(new RaptureFolderInfo(keegan, false));
                }
            }
        }
        list.addAll(map.values());
        return list;
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public List<String> getAllSubKeys(String displayNamePart) {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public void resetFolderHandling() {

    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        IndexHandler indexHandler = new IndexHandler() {
            IndexProducer indexProducer;

            @Override
            public void deleteTable() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void removeAll(String rowId) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setConfig(Map<String, String> config) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public List<TableRecord> queryTable(TableQuery query) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setInstanceName(String instanceName) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public TableQueryResult query(String query) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public Long getLatestEpoch() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setIndexProducer(IndexProducer indexProducer) {
                this.indexProducer = indexProducer;
            }

            @Override
            public void initialize() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void updateRow(String key, Map<String, Object> recordValues) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void ensureIndicesExist() {
                throw new RaptNotSupportedException("Not yet supported");
            }
        };

        indexHandler.setIndexProducer(indexProducer);
        return indexHandler;
    }

    @Override
    public Boolean validate() {
        return true;
    }

    // Not sure what the point of this is
    @Override
    public long getSize() {
        return -1;
    }
}
