package com.sha.perceptdrive.services;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.List;

import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.EntityMention;
import com.google.cloud.language.v1.LanguageServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NaturalLanguageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public NaturalLanguageProcessor() {
    }

    public List<Entity> analyzeEntities(final String textToAnalyze) throws IOException {
        List<Entity> entities;

        try (LanguageServiceClient languageServiceClient = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder().setContent(textToAnalyze).setType(Document.Type.PLAIN_TEXT).build();
            AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder().setDocument(doc).setEncodingType(EncodingType.UTF8).build();

            AnalyzeEntitiesResponse response = languageServiceClient.analyzeEntities(request);
            entities = response.getEntitiesList();
            printEntities(response);
        }
        return entities;
    }

    private void printEntities(final AnalyzeEntitiesResponse response) {
        // Print the response
        for (Entity entity : response.getEntitiesList()) {
            LOG.info("Entity: {}", entity.getName());
            LOG.info("Salience: {}", entity.getSalience());
            LOG.info("Metadata");
            for (Map.Entry<String, String> entry : entity.getMetadataMap().entrySet()) {
                LOG.info("{} : {}", entry.getKey(), entry.getValue());
            }
            for (EntityMention mention : entity.getMentionsList()) {
                LOG.info("Begin offset: {}", mention.getText().getBeginOffset());
                LOG.info("Content: {}", mention.getText().getContent());
                LOG.info("Type: {}", mention.getType());
            }
        }
    }
}
