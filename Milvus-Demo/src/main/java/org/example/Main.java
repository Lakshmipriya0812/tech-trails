package org.example;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.SearchResultsWrapper.IDScore;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Step 1: Connect to Milvus
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("in03-d6ca0c1aef18d05.serverless.gcp-us-west1.cloud.zilliz.com")
                        .withPort(443)
                        .withAuthorization("db_d6ca0c1aef18d05", "Ue7-RAM9^TK6g6Z5") // Replace with your Zilliz creds
                        .withSecure(true)
                        .build()
        );
        System.out.println("✅ Connected to Milvus");

        String collectionName = "hybrid__search_demo";

        // Step 2: Define schema with an additional "text" field
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(4) // 4 for demo; real embeddings = 384, 768, 1536 etc.
                .build();

        FieldType textField = FieldType.newBuilder()
                .withName("text")
                .withDataType(DataType.VarChar)
                .withMaxLength(256)
                .build();

        FieldType categoryField = FieldType.newBuilder()
                .withName("category")
                .withDataType(DataType.VarChar)
                .withMaxLength(100)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("Text vector search demo")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(textField)
                .addFieldType(categoryField)
                .build();

        client.createCollection(createParam);
        System.out.println("✅ Collection created");

        // Step 3: Insert sample text + simulated vectors
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<String> texts = Arrays.asList(
                "apple is a fruit",
                "the sun is bright",
                "i love java programming"
        );
        List<List<Float>> embeddings = Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f),  // fake vector for "apple is a fruit"
                Arrays.asList(0.4f, 0.5f, 0.6f, 0.7f),  // fake vector for "the sun is bright"
                Arrays.asList(0.9f, 0.1f, 0.3f, 0.8f)   // fake vector for "i love java programming"
        );
        List<String> categories = Arrays.asList("fruit", "tech", "tech");

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field("id", ids),
                        new InsertParam.Field("embedding", embeddings), // ✅ Corrected here
                        new InsertParam.Field("text", texts),            // ✅ Also add "text" field
                        new InsertParam.Field("category", categories)
                ))
                .build();



        client.insert(insertParam);
        System.out.println("✅ Text + vector data inserted");

        // Step 4: Create index
        client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\": 128}")
                .withSyncMode(true)
                .build());

        // Step 5: Load collection
        client.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());

        TimeUnit.SECONDS.sleep(2);
        System.out.println("✅ Collection loaded");

        // Step 6: Search with a simulated embedding for "i enjoy coding in java"
        List<Float> queryVector = Arrays.asList(0.88f, 0.12f, 0.25f, 0.75f); // Simulated query vector

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("embedding")
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(2)
                .withMetricType(MetricType.L2)
                .withOutFields(Arrays.asList("id", "text", "category"))
                .withExpr("category == \"fruit\"") // 👈 This is the hybrid filter
                .withParams("{\"nprobe\": 10}")
                .build();

        SearchResults searchResults = client.search(searchParam).getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());
        List<IDScore> results = wrapper.getIDScore(0);

        System.out.println("🔍 Search Results:");
        for (IDScore result : results) {
            long matchedId = result.getLongID();
            float score = result.getScore();
            System.out.printf("Found ID: %d with score: %f%n", matchedId, score);
        }

        // Step 7: Close connection
        client.close();
        System.out.println("👋 Disconnected from Milvus");
    }
}
