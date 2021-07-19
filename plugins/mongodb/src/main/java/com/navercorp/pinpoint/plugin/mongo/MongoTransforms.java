/*
 * Copyright 2020 NAVER Corp.
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

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.ClassFilters;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.ConnectionCloseInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.util.InstrumentUtils;
import com.navercorp.pinpoint.plugin.mongo.field.getter.ExtendedBsonListGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.FieldNameGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.OperatorGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.ValueGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.BsonValueGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.FilterGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.FiltersGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.GeometryGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.InternalOperatorNameAccessor;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.IterableValuesGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.MaxDistanceGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.MinDistanceGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.OperatorNameGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.SearchGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.filters.TextSearchOptionsGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.updates.ListValuesGetter;
import com.navercorp.pinpoint.plugin.mongo.field.getter.updates.PushOptionsGetter;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoCUDSessionInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoDriverConnectInterceptor3_0;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoDriverConnectInterceptor3_7;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoDriverGetCollectionInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoDriverGetDatabaseInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoInternalOperatorNameInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoRSessionInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoReadPreferenceInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.MongoWriteConcernInterceptor;
import com.navercorp.pinpoint.plugin.mongo.interceptor.PublisherInterceptor;

import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.List;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Roy Kim
 * @author yjqg6666
 */
public class MongoTransforms {

    private static final String MONGO_SCOPE = MongoConstants.MONGO_SCOPE;

    public static abstract class AbstractMongoTransformCallback implements TransformCallback {

        private static int MONGO_MAJOR_VERSION = 0;

        public boolean supportedVersion(ClassLoader classLoader) {
            int supportedMajorVersion = 3; // 3.x.x only
            return getMongoMajorVersion(classLoader) == supportedMajorVersion;
        }

        public static int getMongoMajorVersion(ClassLoader classLoader) {
            if (MONGO_MAJOR_VERSION != 0) {
                return MONGO_MAJOR_VERSION;
            }
            int majorVersion;
            try {
                final String versionCheckClassVersionAll = "com.mongodb.MongoException"; //for all versions
                final Class<?> checkClazz = Class.forName(versionCheckClassVersionAll, false, classLoader);
                final String implementationVersion = checkClazz.getPackage().getImplementationVersion();
                if (implementationVersion != null) {
                    final String[] versionsString = implementationVersion.split("\\.");
                    majorVersion = Integer.parseInt(versionsString[0]);
                } else {
                    final String versionCheckClassV1 = "com.mongodb.ByteEncoder"; //only exist in version 1.x
                    final String versionCheckClassV2 = "com.mongodb.Response"; //only exist in version 2.x
                    final String versionCheckClassV3 = "com.mongodb.MongoBulkWriteException"; //only exist in version 3.x
                    if (classExist(classLoader, versionCheckClassV3)) {
                        majorVersion = 3;
                    } else {
                        if (classExist(classLoader, versionCheckClassV2)) {
                            majorVersion = 2;
                        } else {
                            if (classExist(classLoader, versionCheckClassV1)) {
                                majorVersion = 1;
                            } else {
                                majorVersion = -1;
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                majorVersion = -2;
            } catch (NumberFormatException e) {
                majorVersion = -3;
            } catch (Exception e) {
                majorVersion = -4;
            }
            MONGO_MAJOR_VERSION = majorVersion;
            return majorVersion;
        }

        private static boolean classExist(ClassLoader classLoader, String className) {
            try {
                Class.forName(className, false, classLoader);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

    }

    //@TODO how about. pullByFilter under Updates
    public static class FilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            List<InstrumentClass> nestedClasses = target.getNestedClasses(
                    ClassFilters.name(MongoConstants.FILTERLIST.toArray(new String[0]))
            );

            for (final InstrumentClass nestedClass : nestedClasses) {

                //GeometryOperatorFilter 3.1+
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_GEOMETRYOPERATOR)) {
                    instrumentor.transform(loader, nestedClass.getName(), GeometryOperatorTransform.class);
                }

                //NotFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_NOT)) {
                    instrumentor.transform(loader, nestedClass.getName(), NotFilterTransform.class);
                }

                //SimpleEncodingFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_SIMPLEENCODING)) {
                    instrumentor.transform(loader, nestedClass.getName(), SimpleEncodingFilterTransform.class);
                }

                //IterableOperatorFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_ITERABLEOPERATOR)) {
                    instrumentor.transform(loader, nestedClass.getName(), IterableOperatorFilterTransform.class);
                }

                //OrFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_OR)) {
                    instrumentor.transform(loader, nestedClass.getName(), OrFilterTransform.class);
                }

                //AndFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_AND)) {
                    instrumentor.transform(loader, nestedClass.getName(), AndFilterTransform.class);
                }

                //OperatorFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_OPERATOR)) {
                    instrumentor.transform(loader, nestedClass.getName(), OperatorFilterTransform.class);
                }

                //SimpleFilter
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_SIMPLE)) {
                    instrumentor.transform(loader, nestedClass.getName(), SimpleFilterTransform.class);
                }

                //TextFilter 3.3+
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_TEXT)) {
                    instrumentor.transform(loader, nestedClass.getName(), TextFilterTransform.class);
                }

                //OrNorFilter 3.3+
                if (nestedClass.getName().equals(MongoConstants.MONGO_FILTER_ORNOR)) {
                    instrumentor.transform(loader, nestedClass.getName(), OrNorFilterTransform.class);
                }
            }
            return target.toBytecode();
        }
    }

    public static class GeometryOperatorTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(OperatorNameGetter.class, "operatorName");
            nestedTarget.addGetter(FilterGetter.class, "fieldName");
            nestedTarget.addGetter(GeometryGetter.class, "geometry");
            nestedTarget.addGetter(MaxDistanceGetter.class, "maxDistance");
            nestedTarget.addGetter(MinDistanceGetter.class, "minDistance");

            return nestedTarget.toBytecode();
        }
    }

    public static class AndFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(FiltersGetter.class, "filters");
            return nestedTarget.toBytecode();
        }
    }

    public static class ClientConnectionTransform3_0_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findConstructor(target, "com.mongodb.connection.Cluster", "com.mongodb.MongoClientOptions", "java.util.List");

            connect.addScopedInterceptor(MongoDriverConnectInterceptor3_0.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);


            InstrumentMethod close = InstrumentUtils.findMethod(target, "close");
            close.addScopedInterceptor(ConnectionCloseInterceptor.class, MONGO_SCOPE);

            InstrumentMethod getDB = target.getDeclaredMethod("getDB", "java.lang.String");
            if (getDB != null) {
                getDB.addScopedInterceptor(MongoDriverGetDatabaseInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }
    }

    public static class DatabaseConnectionTransform3_0_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connectDeliver = InstrumentUtils.findMethod(target, "getDatabase", "java.lang.String");
            connectDeliver.addScopedInterceptor(MongoDriverGetDatabaseInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            return target.toBytecode();
        }
    }

    public static class CollectionConnectionTransform3_0_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findMethod(target, "getCollection", "java.lang.String", "java.lang.Class");
            connect.addScopedInterceptor(MongoDriverGetCollectionInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod getReadPreference = InstrumentUtils.findMethod(target, "withReadPreference", "com.mongodb.ReadPreference");
            getReadPreference.addScopedInterceptor(MongoReadPreferenceInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod getWriteConcern = InstrumentUtils.findMethod(target, "withWriteConcern", "com.mongodb.WriteConcern");
            getWriteConcern.addScopedInterceptor(MongoWriteConcernInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            return target.toBytecode();
        }
    }

    public static class ClientConnectionTransform3_7_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findMethod(target, "create", "com.mongodb.MongoClientSettings", "com.mongodb.MongoDriverInformation");

            connect.addScopedInterceptor(MongoDriverConnectInterceptor3_7.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            return target.toBytecode();
        }
    }

    public static class DatabaseConnectionTransform3_7_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findMethod(target, "getDatabase", "java.lang.String");
            connect.addScopedInterceptor(MongoDriverGetDatabaseInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod close = InstrumentUtils.findMethod(target, "close");
            close.addScopedInterceptor(ConnectionCloseInterceptor.class, MONGO_SCOPE);

            return target.toBytecode();
        }
    }

    public static class CollectionConnectionTransform3_7_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findMethod(target, "getCollection", "java.lang.String", "java.lang.Class");
            connect.addScopedInterceptor(MongoDriverGetCollectionInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);


            InstrumentMethod getReadPreference = InstrumentUtils.findMethod(target, "withReadPreference", "com.mongodb.ReadPreference");
            getReadPreference.addScopedInterceptor(MongoReadPreferenceInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod getWriteConcern = InstrumentUtils.findMethod(target, "withWriteConcern", "com.mongodb.WriteConcern");
            getWriteConcern.addScopedInterceptor(MongoWriteConcernInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            return target.toBytecode();
        }
    }

    public static class DatabaseConnectionTransform3_8_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod connect = InstrumentUtils.findMethod(target, "getDatabase", "java.lang.String");
            connect.addScopedInterceptor(MongoDriverGetDatabaseInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod close = InstrumentUtils.findMethod(target, "close");
            close.addScopedInterceptor(ConnectionCloseInterceptor.class, MONGO_SCOPE);

            return target.toBytecode();
        }
    }

    public static class SessionTransform3_0_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            target.addField(DatabaseInfoAccessor.class);

            MongoConfig config = new MongoConfig(instrumentor.getProfilerConfig());

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getMethodListR3_0_x())))) {
                method.addScopedInterceptor(MongoRSessionInterceptor.class, va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getMethodListCUD3_0_x())))) {
                method.addScopedInterceptor(MongoCUDSessionInterceptor.class, va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            InstrumentMethod getReadPreference = InstrumentUtils.findMethod(target, "withReadPreference", "com.mongodb.ReadPreference");
            getReadPreference.addScopedInterceptor(MongoReadPreferenceInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod getWriteConcern = InstrumentUtils.findMethod(target, "withWriteConcern", "com.mongodb.WriteConcern");
            getWriteConcern.addScopedInterceptor(MongoWriteConcernInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            return target.toBytecode();
        }
    }

    public static class SessionTransform3_7_X extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            MongoConfig config = new MongoConfig(instrumentor.getProfilerConfig());

            target.addField(DatabaseInfoAccessor.class);

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getMethodListR3_7_x())))) {
                method.addScopedInterceptor(MongoRSessionInterceptor.class, va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getMethodListCUD3_7_x())))) {
                method.addScopedInterceptor(MongoCUDSessionInterceptor.class, va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            InstrumentMethod getReadPreference = InstrumentUtils.findMethod(target, "withReadPreference", "com.mongodb.ReadPreference");
            getReadPreference.addScopedInterceptor(MongoReadPreferenceInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);

            InstrumentMethod getWriteConcern = InstrumentUtils.findMethod(target, "withWriteConcern", "com.mongodb.WriteConcern");
            getWriteConcern.addScopedInterceptor(MongoWriteConcernInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);


            return target.toBytecode();
        }
    }

    public static class ObservableToPublisherTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            final InstrumentMethod subscribeMethod = target.getDeclaredMethod("subscribe", "org.reactivestreams.Subscriber");
            if (subscribeMethod != null) {
                subscribeMethod.addInterceptor(PublisherInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    public static class NotFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(FilterGetter.class, "filter");
            return nestedTarget.toBytecode();
        }
    }

    public static class SimpleEncodingFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(ValueGetter.class, "value");
            return nestedTarget.toBytecode();
        }
    }

    public static class IterableOperatorFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            if (nestedTarget.hasField("fieldName")) {
                nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            }
            nestedTarget.addGetter(OperatorNameGetter.class, "operatorName");
            nestedTarget.addGetter(IterableValuesGetter.class, "values");
            return nestedTarget.toBytecode();
        }
    }

    public static class OrFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(FiltersGetter.class, "filters");
            return nestedTarget.toBytecode();
        }
    }

    public static class OperatorFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(OperatorNameGetter.class, "operatorName");
            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(ValueGetter.class, "value");
            return nestedTarget.toBytecode();
        }
    }

    public static class SimpleFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(BsonValueGetter.class, "value");
            return nestedTarget.toBytecode();
        }
    }

    public static class TextFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(SearchGetter.class, "search");
            nestedTarget.addGetter(TextSearchOptionsGetter.class, "textSearchOptions");
            return nestedTarget.toBytecode();
        }
    }

    public static class OrNorFilterTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }

            nestedTarget.addGetter(FiltersGetter.class, "filters");
            nestedTarget.addField(InternalOperatorNameAccessor.class);

            final InstrumentMethod nestedConstructor = nestedTarget.getConstructor("com.mongodb.client.model.Filters$OrNorFilter$Operator", "java.lang.Iterable");

            if (nestedConstructor != null) {
                nestedConstructor.addInterceptor(MongoInternalOperatorNameInterceptor.class);
            }

            return nestedTarget.toBytecode();
        }
    }

    public static class UpdatesTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            List<InstrumentClass> nestedClasses = target.getNestedClasses(
                    ClassFilters.name(MongoConstants.UPDATESLIST.toArray(new String[0]))
            );

            for (final InstrumentClass nestedClass : nestedClasses) {

                //SimpleUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_UPDATES_SIMPLE)) {
                    instrumentor.transform(loader, nestedClass.getName(), MongoTransforms.SimpleUpdateTransform.class);
                }

                //WithEachUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_UPDATES_WITHEACH)) {
                    instrumentor.transform(loader, nestedClass.getName(), MongoTransforms.WithEachUpdateTransform.class);
                }

                //PushUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_UPDATES_PUSH)) {
                    instrumentor.transform(loader, nestedClass.getName(), MongoTransforms.PushUpdateTransform.class);
                }

                //PullAllUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_UPDATES_PULLALL)) {
                    instrumentor.transform(loader, nestedClass.getName(), MongoTransforms.PullAllUpdateTransform.class);
                }

                //CompositeUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_UPDATES_COMPOSITE)) {
                    instrumentor.transform(loader, nestedClass.getName(), MongoTransforms.CompositeUpdateTransform.class);
                }
            }
            return target.toBytecode();
        }
    }

    public static class SimpleUpdateTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }
            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(ValueGetter.class, "value");
            nestedTarget.addGetter(OperatorGetter.class, "operator");
            return nestedTarget.toBytecode();
        }
    }

    public static class WithEachUpdateTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }
            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(ListValuesGetter.class, "values");
            nestedTarget.addGetter(OperatorGetter.class, "operator");
            return nestedTarget.toBytecode();
        }
    }

    public static class PushUpdateTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }
            nestedTarget.addGetter(PushOptionsGetter.class, "options");
            return nestedTarget.toBytecode();
        }
    }

    public static class PullAllUpdateTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }
            nestedTarget.addGetter(FieldNameGetter.class, "fieldName");
            nestedTarget.addGetter(ListValuesGetter.class, "values");
            return nestedTarget.toBytecode();
        }
    }

    public static class CompositeUpdateTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!nestedTarget.isInterceptable()) {
                return null;
            }
            nestedTarget.addGetter(ExtendedBsonListGetter.class, "updates");
            return nestedTarget.toBytecode();
        }
    }

    public static class SortsTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            if (!supportedVersion(loader)) {
                return null;
            }
            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            List<InstrumentClass> nestedClasses = target.getNestedClasses(
                    ClassFilters.name(MongoConstants.MONGO_SORT_COMPOSITE)
            );

            for (final InstrumentClass nestedClass : nestedClasses) {

                //CompositeUpdate
                if (nestedClass.getName().equals(MongoConstants.MONGO_SORT_COMPOSITE)) {
                    instrumentor.transform(loader, nestedClass.getName(), new AbstractMongoTransformCallback() {
                        @Override
                        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                            if (!supportedVersion(loader)) {
                                return null;
                            }
                            final InstrumentClass nestedTarget = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                            if (!nestedTarget.isInterceptable()) {
                                return null;
                            }
                            nestedTarget.addGetter(ExtendedBsonListGetter.class, "updates");
                            return nestedTarget.toBytecode();
                        }
                    });
                }
            }
            return target.toBytecode();
        }
    }

    public static class DBCollectionTransform extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws InstrumentException {

            if (!supportedVersion(loader)) {
                return null;
            }

            final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!target.isInterceptable()) {
                return null;
            }

            MongoConfig config = new MongoConfig(instrumentor.getProfilerConfig());

            target.addField(DatabaseInfoAccessor.class);
            List<InstrumentMethod> declaredReadMethods = target.getDeclaredMethods(MethodFilters
                    .chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getDBCollectionReadMethodList())));

            for (InstrumentMethod method : declaredReadMethods) {
                method.addScopedInterceptor(MongoRSessionInterceptor.class,
                        va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE,
                        ExecutionPolicy.BOUNDARY);
            }

            List<InstrumentMethod> declaredCUDMethods = target.getDeclaredMethods(MethodFilters
                    .chain(MethodFilters.modifier(Modifier.PUBLIC), MethodFilters.name(getDBCollectionCUDMethodList())));
            for (InstrumentMethod method : declaredCUDMethods) {
                method.addScopedInterceptor(MongoCUDSessionInterceptor.class,
                        va(config.isCollectJson(), config.istraceBsonBindValue()), MONGO_SCOPE,
                        ExecutionPolicy.BOUNDARY);
            }

            InstrumentMethod setReadPreferenceMethod = target.getDeclaredMethod("setReadPreference",
                    "com.mongodb.ReadPreference");
            if (setReadPreferenceMethod != null) {
                setReadPreferenceMethod.addScopedInterceptor(MongoReadPreferenceInterceptor.class, MONGO_SCOPE,
                        ExecutionPolicy.BOUNDARY);
            }

            InstrumentMethod writeConcernMethod = target.getDeclaredMethod("setWriteConcern",
                    "com.mongodb.WriteConcern");
            if (writeConcernMethod != null) {
                writeConcernMethod.addScopedInterceptor(MongoWriteConcernInterceptor.class, MONGO_SCOPE,
                        ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }
    }

    public static class DBTransformer extends AbstractMongoTransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className,
                                    Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws InstrumentException {

            if (!supportedVersion(loader)) {
                return null;
            }

            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            if (!target.isInterceptable()) {
                return null;
            }
            target.addField(DatabaseInfoAccessor.class);

            InstrumentMethod collection = target.getDeclaredMethod("getCollection", "java.lang.String");
            if (collection != null) {
                collection.addScopedInterceptor(MongoDriverGetCollectionInterceptor.class, MONGO_SCOPE, ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }
    }

    private static String[] getDBCollectionCUDMethodList() {
        return new String[]{"insert", "save", "replaceOrInsert", "update", "updateMulti", "remove",
                "rename", "createIndex", "drop", "dropIndex", "dropIndexes"};
    }

    private static String[] getDBCollectionReadMethodList() {
        return new String[]{"find", "findOne", "count", "distinct", "getCount", "findAndModify"};
    }

    private static String[] getMethodListR3_0_x() {
        return new String[]{"findOneAndUpdate", "findOneAndReplace", "findOneAndDelete", "find", "count", "distinct", "listIndexes"
//                , "watch" ,"aggregate","mapReduce"
        };
    }

    private static String[] getMethodListCUD3_0_x() {
        return new String[]{"dropIndexes", "dropIndex", "createIndexes", "createIndex", "updateMany", "updateOne", "replaceOne", "deleteMany", "deleteOne", "insertMany", "insertOne", "bulkWrite"};
    }

    private static String[] getMethodListR3_7_x() {
        return new String[]{"findOneAndUpdate", "findOneAndReplace", "findOneAndDelete", "find", "count", "distinct", "listIndexes", "countDocuments"
//                , "watch", "aggregate", "mapReduce"
        };
    }

    private static String[] getMethodListCUD3_7_x() {
        return new String[]{"dropIndexes", "dropIndex", "createIndexes", "createIndex", "updateMany", "updateOne", "replaceOne", "deleteMany", "deleteOne", "insertMany", "insertOne", "bulkWrite"};
    }

}
