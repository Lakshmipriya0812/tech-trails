package org.example;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.param.partition.HasPartitionParam;
import io.milvus.param.partition.LoadPartitionsParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.SearchResultsWrapper.IDScore;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Partition {
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

        String collectionName = "partition_demo_collection";
        String partitionName = "partition_tech";

        // Create collection (if not exist)
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
                .withDescription("Partition demo")
                .withShardsNum(2)
                .addFieldType(idField)
                .addFieldType(vectorField)
                .build();

        client.createCollection(createParam);
        System.out.println("✅ Collection created"+collectionName);

        // Create partition
        client.createPartition(CreatePartitionParam.newBuilder()
                .withCollectionName(collectionName)
                .withPartitionName(partitionName)
                .build());
        System.out.println("✅ Partition created: " + partitionName);

        // Check if partition exists
        boolean hasPartition = client.hasPartition(HasPartitionParam.newBuilder()
                .withCollectionName(collectionName)
                .withPartitionName(partitionName)
                .build()).getData();
        System.out.println("Partition exists? " + hasPartition);

        // Insert data into partition
        List<Long> ids = Arrays.asList(1L, 2L);
        List<List<Float>> vectors = Arrays.asList(
                Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f),
                Arrays.asList(0.4f, 0.5f, 0.6f, 0.7f)
        );

        client.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withPartitionName(partitionName)
                .withFields(Arrays.asList(
                        new InsertParam.Field("id", ids),
                        new InsertParam.Field("embedding", vectors)
                ))
                .build());
        System.out.println("✅ Data inserted into partition");

        client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding") // name of the vector field
                .withIndexType(IndexType.IVF_FLAT) // or HNSW, IVF_SQ8, etc.
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\": 128}") // required for IVF index types
                .withSyncMode(true) // wait for indexing to complete
                .build());
        System.out.println("✅ Index created");

        // Load partition before search
        client.loadPartitions(LoadPartitionsParam.newBuilder()
                .withCollectionName(collectionName)
                .withPartitionNames(Collections.singletonList(partitionName))
                .build());
        System.out.println("✅ Partition loaded");

        TimeUnit.SECONDS.sleep(2);

        // Search in the partition
        List<Float> queryVector = Arrays.asList(0.15f, 0.25f, 0.35f, 0.45f);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("embedding")
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(2)
                .withMetricType(io.milvus.param.MetricType.L2)
                .withPartitionNames(Collections.singletonList(partitionName)) // restrict search to this partition
                .build();


        var response = client.search(searchParam);
        if (response.getData() == null) {
            System.err.println("❌ Search failed: " + response.getException().getMessage());
            client.close();
            return;
        }

        var searchResults = client.search(searchParam).getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());
        List<IDScore> results = wrapper.getIDScore(0);

        System.out.println("🔍 Search Results in partition:");
        for (IDScore result : results) {
            System.out.printf("Found ID: %d with score: %f%n", result.getLongID(), result.getScore());
        }

        client.close();
        System.out.println("👋 Disconnected");
    }
}
