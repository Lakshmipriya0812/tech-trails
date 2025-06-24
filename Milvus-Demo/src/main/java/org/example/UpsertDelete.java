package org.example;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.SearchResultsWrapper.IDScore;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpsertDelete {
    public static void main(String[] args) throws InterruptedException {
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("in03-d6ca0c1aef18d05.serverless.gcp-us-west1.cloud.zilliz.com")
                        .withPort(443)
                        .withAuthorization("db_d6ca0c1aef18d05", "Ue7-RAM9^TK6g6Z5")
                        .withSecure(true)
                        .build()
        );
        System.out.println("✅ Connected to Milvus");

        String collectionName = "upsert_delete_demo";

        // Create collection if needed
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(4)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Upsert and Delete demo")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(vectorField)
                .build();

        client.createCollection(createParam);
        System.out.println("✅ Collection created");

        // Insert initial data
        List<Long> ids = Arrays.asList(1L, 2L);
        List<List<Float>> vectors = Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f),
                Arrays.asList(0.4f, 0.5f, 0.6f, 0.7f)
        );

        client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT) // or HNSW, depending on your needs
                .withMetricType(MetricType.L2)     // or COSINE/INNER_PRODUCT
                .withExtraParam("{\"nlist\": 128}")
                .withSyncMode(true)
                .build());
        System.out.println("✅ Index created");


        // Delete vector with id=1
        client.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr("id in [1]")
                .build());
        System.out.println("✅ Deleted data with id=1");

        // Insert updated data (simulate upsert)
        List<Long> newIds = Arrays.asList(1L, 3L);
        List<List<Float>> newVectors = Arrays.asList(
                Arrays.asList(0.9f, 0.8f, 0.7f, 0.6f), // updated vector for id=1
                Arrays.asList(0.5f, 0.4f, 0.3f, 0.2f)  // new vector for id=3
        );

        client.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field("id", newIds),
                        new InsertParam.Field("embedding", newVectors)
                ))
                .build());
        System.out.println("✅ Inserted updated/new data (upsert simulation)");

        // Load collection before search
        client.loadCollection(io.milvus.param.collection.LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        TimeUnit.SECONDS.sleep(2);

        // Search to verify
        List<Float> queryVector = Arrays.asList(0.9f, 0.8f, 0.7f, 0.6f);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("embedding")
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(3)
                .withMetricType(io.milvus.param.MetricType.L2)
                .withOutFields(Collections.singletonList("id"))
                .build();

        var response = client.search(searchParam);
        if (response.getData() == null) {
            System.err.println("❌ Search failed, no data returned.");
            return;
        }
        SearchResults searchResults = response.getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());
        List<IDScore> results = wrapper.getIDScore(0);

        System.out.println("🔍 Search Results after upsert:");
        for (IDScore result : results) {
            System.out.printf("Found ID: %d with score: %f%n", result.getLongID(), result.getScore());
        }

        client.close();
        System.out.println("👋 Disconnected");
    }
}
