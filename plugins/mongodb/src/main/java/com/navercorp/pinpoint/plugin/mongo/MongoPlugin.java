/*
 * Copyright 2018 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.mongo;

import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * @author Roy Kim
 */
public class MongoPlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        MongoConfig config = new MongoConfig(context.getConfig());
        if (!config.isEnable()) {
            logger.info("{} disabled", this.getClass().getSimpleName());
            return;
        }
        logger.info("{} config:{}", this.getClass().getSimpleName(), config);

        addFilterTransformer();
        addUpdatesTransformer();
        //TODO withReadConcern
        //TODO SimpleExpression
        //TODO Sort, Projection
//        addSortsTransformer();
//        addProjectionTransformer();

        addConnectionTransformer3_0_X();
        addConnectionTransformer3_7_X();
        addConnectionTransformer3_8_X();
        addConnectionTransformerReactive();
        addSessionTransformer3_0_X();
        addSessionTransformer3_7_X();
        // Reactive stream
        addSessionTransformerReactive();
        //mongo DB/DBCollection
        addDBCollectionTransformer();
        addDBTransformer();
    }

    private void addConnectionTransformer3_0_X() {

        // 3.0.0 ~ 3.6.4
        transformTemplate.transform("com.mongodb.Mongo", MongoTransforms.ClientConnectionTransform3_0_X.class);
        transformTemplate.transform("com.mongodb.MongoClient", MongoTransforms.DatabaseConnectionTransform3_0_X.class);
        transformTemplate.transform("com.mongodb.MongoDatabaseImpl", MongoTransforms.CollectionConnectionTransform3_0_X.class);

    }

    private void addConnectionTransformer3_7_X() {
        //3.7.0+
        transformTemplate.transform("com.mongodb.client.MongoClients", MongoTransforms.ClientConnectionTransform3_7_X.class);
        transformTemplate.transform("com.mongodb.client.MongoClientImpl", MongoTransforms.DatabaseConnectionTransform3_7_X.class);
        transformTemplate.transform("com.mongodb.client.internal.MongoDatabaseImpl", MongoTransforms.CollectionConnectionTransform3_7_X.class);
    }

    private void addConnectionTransformer3_8_X() {
        //3.8.0+
        transformTemplate.transform("com.mongodb.client.internal.MongoClientImpl", MongoTransforms.DatabaseConnectionTransform3_8_X.class);
    }

    private void addConnectionTransformerReactive() {
        //reactivestreams
        transformTemplate.transform("com.mongodb.reactivestreams.client.MongoClients", MongoTransforms.ClientConnectionTransform3_7_X.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.MongoClientImpl", MongoTransforms.DatabaseConnectionTransform3_8_X.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.MongoDatabaseImpl", MongoTransforms.CollectionConnectionTransform3_7_X.class);
    }


    private void addSessionTransformer3_0_X() {
        transformTemplate.transform("com.mongodb.MongoCollectionImpl", MongoTransforms.SessionTransform3_0_X.class);
    }

    private void addSessionTransformer3_7_X() {
        transformTemplate.transform("com.mongodb.client.internal.MongoCollectionImpl", MongoTransforms.SessionTransform3_7_X.class);
    }

    private void addSessionTransformerReactive() {
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.MongoCollectionImpl", MongoTransforms.SessionTransform3_7_X.class);

        // Reactive
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.FindPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.AggregatePublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.ChangeStreamPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.DistinctPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.GridFSDownloadPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.GridFSFindPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.GridFSUploadPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.ListCollectionsPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.ListDatabasesPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.ListIndexesPublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.MapReducePublisherImpl", MongoTransforms.ObservableToPublisherTransform.class);
        // 1.12
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.SingleResultObservableToPublisher", MongoTransforms.ObservableToPublisherTransform.class);
        // 1.13
        transformTemplate.transform("com.mongodb.reactivestreams.client.internal.ObservableToPublisher", MongoTransforms.ObservableToPublisherTransform.class);
    }

    private void addFilterTransformer() {
        transformTemplate.transform("com.mongodb.client.model.Filters", MongoTransforms.FilterTransform.class);
    }

    private void addUpdatesTransformer() {
        transformTemplate.transform("com.mongodb.client.model.Updates", MongoTransforms.UpdatesTransform.class);
    }

    private void addSortsTransformer() {
        transformTemplate.transform("com.mongodb.client.model.Sorts", MongoTransforms.SortsTransform.class);
    }

    private void addDBCollectionTransformer() {
        transformTemplate.transform("com.mongodb.DBCollection", MongoTransforms.DBCollectionTransform.class);
    }

    private void addDBTransformer() {
        transformTemplate.transform("com.mongodb.DB", MongoTransforms.DBTransformer.class);
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
