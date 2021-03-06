/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langserver.extensions.ballerina.connector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.balo.BaloProject;
import io.ballerina.projects.repos.TempDirCompilationCache;
import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.diagramutil.DiagramUtil;
import org.ballerinalang.langserver.LSGlobalContext;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.exception.LSConnectorException;
import org.ballerinalang.model.elements.PackageID;
import org.eclipse.lsp4j.Position;
import org.wso2.ballerinalang.compiler.packaging.Patten;
import org.wso2.ballerinalang.compiler.packaging.repo.HomeBaloRepo;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.ProjectDirConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.ballerinalang.langserver.compiler.LSClientLogger.logError;

/**
 * Implementation of the BallerinaConnectorService.
 *
 * @since 2.0.0
 */
public class BallerinaConnectorServiceImpl implements BallerinaConnectorService {

    public static final String DEFAULT_CONNECTOR_FILE_KEY = "DEFAULT_CONNECTOR_FILE";
    private static final Path STD_LIB_SOURCE_ROOT = Paths.get(CommonUtil.BALLERINA_HOME)
            .resolve("repo")
            .resolve("balo");
    private String connectorConfig;
    private LSGlobalContext lsContext;
    private WorkspaceManager workspaceManager;

    public BallerinaConnectorServiceImpl(WorkspaceManager workspaceManager, LSGlobalContext lsContext) {
        this.workspaceManager = workspaceManager;
        this.lsContext = lsContext;
        connectorConfig = System.getenv(DEFAULT_CONNECTOR_FILE_KEY);
        if (connectorConfig == null) {
            connectorConfig = System.getProperty(DEFAULT_CONNECTOR_FILE_KEY);
        }
    }

    @Override
    public CompletableFuture<BallerinaConnectorsResponse> connectors() {
        try {
            BallerinaConnectorsResponse response = getConnectorConfig();
            return CompletableFuture.supplyAsync(() -> response);
        } catch (IOException e) {
            String msg = "Operation 'ballerinaConnector/connectors' failed!";
            logError(msg, e, null, (Position) null);
        }

        return CompletableFuture.supplyAsync(BallerinaConnectorsResponse::new);
    }

    private Path getBaloPath(String org, String module, String version) throws LSConnectorException {
        Path baloPath = STD_LIB_SOURCE_ROOT.resolve(org).resolve(module).
                resolve(version.isEmpty() ?
                        ProjectDirConstants.BLANG_PKG_DEFAULT_VERSION : version).
                resolve(String.format("%s-%s-any-%s%s", org, module, version,
                        ProjectDirConstants.BLANG_COMPILED_PKG_BINARY_EXT));
        if (!Files.exists(baloPath.toAbsolutePath())) {
            //check external modules
            PackageID packageID = new PackageID(new Name(org), new Name(module), new Name(version));
            HomeBaloRepo homeBaloRepo = new HomeBaloRepo(new HashMap<>());
            Patten patten = homeBaloRepo.calculate(packageID);
            Stream<Path> s = patten.convert(new BaloConverter(), packageID);
            Optional<Path> path = s.reduce(Path::resolve);
            if (path.isPresent() && Files.exists(path.get().toAbsolutePath())) {
                baloPath = path.get().toAbsolutePath();
            } else {
                throw new LSConnectorException("No file exist in '" + ProjectDirConstants.BLANG_COMPILED_PKG_BINARY_EXT
                        + path.get().toAbsolutePath() + "'");
            }
        }
        return baloPath;
    }

    @Override
    public CompletableFuture<BallerinaConnectorResponse> connector(BallerinaConnectorRequest request) {

        String cacheableKey = getCacheableKey(request.getOrg(), request.getModule(), request.getVersion());
        LSConnectorCache connectorCache = LSConnectorCache.getInstance(lsContext);
        JsonElement st = connectorCache
                .getConnectorConfig(request.getOrg(), request.getModule(), request.getVersion(), request.getName());
        String error = "";
        if (st == null) {
            try {
                Path baloPath = getBaloPath(request.getOrg(), request.getModule(), request.getVersion());

                ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
                defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
                BaloProject baloProject = BaloProject.loadProject(defaultBuilder, baloPath);
                ModuleId moduleId = baloProject.currentPackage().moduleIds().stream().findFirst().get();
                Module module = baloProject.currentPackage().module(moduleId);
                SemanticModel semanticModel = module.getCompilation().getSemanticModel();

                ConnectorNodeVisitor connectorNodeVisitor = new ConnectorNodeVisitor(request.getName(), semanticModel);
                module.documentIds().forEach(documentId -> {
                    module.document(documentId).syntaxTree().rootNode().accept(connectorNodeVisitor);
                });

                Map<String, TypeDefinitionNode> jsonRecords = new HashMap<>();
                connectorNodeVisitor.getRecords().forEach(jsonRecords::put);

                Gson gson = new Gson();
                List<ClassDefinitionNode> connectorNodes = connectorNodeVisitor.getConnectors();

                connectorNodes.forEach(connector -> {
                    // todo : preserve the existing logic add the information to the typeData element of
                    //  the syntax tree JSON and send to front end
                    Map<String, JsonElement> connectorRecords = new HashMap<>();

                    for (Node child : connector.members()) {
                        if (child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) child;
                            functionDefinitionNode.functionSignature().parameters().forEach(parameterNode -> {
                                populateConnectorFunctionParamRecords(parameterNode, semanticModel, jsonRecords,
                                        connectorRecords);
                            });
                        }
                    }

                    JsonElement jsonST = DiagramUtil.getClassDefinitionSyntaxJson(connector, semanticModel);
                    if (jsonST instanceof JsonObject) {
                        JsonElement recordsJson = gson.toJsonTree(connectorRecords);
                        ((JsonObject) jsonST).add("records", recordsJson);
                    }
                    connectorCache.addConnectorConfig(request.getOrg(), request.getModule(),
                            request.getVersion(), connector.className().text(), jsonST);

                });
                st = connectorCache.getConnectorConfig(request.getOrg(), request.getModule(),
                        request.getVersion(), request.getName());
            } catch (Exception e) {
                String msg = "Operation 'ballerinaConnector/connector' for " + cacheableKey + ":" +
                        request.getName() + " failed!";
                error = e.getMessage();
                logError(msg, e, null, (Position) null);
            }
        }
        BallerinaConnectorResponse response = new BallerinaConnectorResponse(request.getOrg(), request.getModule(),
                request.getVersion(), request.getName(), request.getDisplayName(), st, error, request.getBeta());
        return CompletableFuture.supplyAsync(() -> response);
    }

    private void populateConnectorFunctionParamRecords(Node parameterNode, SemanticModel semanticModel,
                                                       Map<String, TypeDefinitionNode> jsonRecords,
                                                       Map<String, JsonElement> connectorRecords) {
        Optional<TypeSymbol> paramType = semanticModel
                .type(parameterNode.syntaxTree().filePath(), parameterNode.lineRange());
        if (paramType.isPresent()) {
            if (paramType.get().typeKind() == TypeDescKind.UNION) {
                Arrays.stream(paramType.get().signature().split("\\|")).forEach(type -> {
                    /* todo : need a better way to get types other than string splitting and checking regex */
                    String typeName = type;
                    Pattern typeRefPattern = Pattern.compile("\\w+/\\w+:[\\d.]+:(\\w+)");
                    Matcher typeRefPatternMatcher = typeRefPattern.matcher(type);
                    if (typeRefPatternMatcher.find()) {
                        typeName = typeRefPatternMatcher.group(1);
                    }
                    TypeDefinitionNode record = jsonRecords.get(typeName);
                    if (record != null) {
                        connectorRecords.put(typeName, DiagramUtil.getTypeDefinitionSyntaxJson(record, semanticModel));
                        populateConnectorRecords(record, semanticModel, jsonRecords, connectorRecords);
                    }
                });
            } else if (paramType.get().typeKind() == TypeDescKind.ARRAY) {
                Pattern arraySignaturePattern = Pattern.compile("(\\w+)\\[\\]$");
                Matcher signatureMatcher = arraySignaturePattern.matcher(paramType.get().signature());

                if (signatureMatcher.find()) {
                    // there is only one group in the regex and array signature always matches
                    TypeDefinitionNode record = jsonRecords.get(signatureMatcher.group(1));
                    if (record != null) {
                        connectorRecords.put(signatureMatcher.group(1),
                                DiagramUtil.getTypeDefinitionSyntaxJson(record, semanticModel));
                        populateConnectorRecords(record, semanticModel, jsonRecords, connectorRecords);
                    }
                }

            }
        }
    }

    private void populateConnectorRecords(TypeDefinitionNode recordTypeDefinition, SemanticModel semanticModel,
                                          Map<String, TypeDefinitionNode> jsonRecords,
                                          Map<String, JsonElement> connectorRecords) {
        RecordTypeDescriptorNode recordTypeDescriptorNode = (RecordTypeDescriptorNode) recordTypeDefinition
                                                                                            .typeDescriptor();

        recordTypeDescriptorNode.fields().forEach(field -> {
            Optional<TypeSymbol> fieldType = semanticModel.type(field.syntaxTree().filePath(), field.lineRange());

            if (fieldType.isPresent() && fieldType.get().typeKind() == TypeDescKind.TYPE_REFERENCE) {
                String type = fieldType.get().signature();
                String typeName = type;
                Pattern typeRefPattern = Pattern.compile("\\w+/\\w+:[\\d.]+:(\\w+)");
                Matcher typeRefPatternMatcher = typeRefPattern.matcher(typeName);

                if (typeRefPatternMatcher.find()) {
                    typeName = typeRefPatternMatcher.group(1);
                }

                TypeDefinitionNode record = jsonRecords.get(typeName);
                if (record != null && !recordTypeDefinition.typeName().text().equals(typeName)) {
                    connectorRecords.put(typeName, DiagramUtil.getSyntaxTreeJSON(record.syntaxTree(), semanticModel));
                    populateConnectorRecords(record, semanticModel, jsonRecords, connectorRecords);
                }
            }
        });
    }

    @Override
    public CompletableFuture<BallerinaRecordResponse> record(BallerinaRecordRequest request) {
        String cacheableKey = getCacheableKey(request.getOrg(), request.getModule(), request.getVersion());
        LSRecordCache recordCache = LSRecordCache.getInstance(lsContext);

        JsonElement ast = recordCache.getRecordAST(request.getOrg(), request.getModule(),
                request.getVersion(), request.getName());
        String error = "";
        if (ast == null) {
            try {
                Path baloPath = getBaloPath(request.getOrg(), request.getModule(), request.getVersion());
                ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
                defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
                BaloProject baloProject = BaloProject.loadProject(defaultBuilder, baloPath);
                ModuleId moduleId = baloProject.currentPackage().moduleIds().stream().findFirst().get();
                Module module = baloProject.currentPackage().module(moduleId);
                SemanticModel semanticModel = module.getCompilation().getSemanticModel();

                Map<String, JsonElement> recordDefJsonMap = new HashMap<>();
                ConnectorNodeVisitor connectorNodeVisitor = new ConnectorNodeVisitor(request.getName(), semanticModel);
                module.documentIds().forEach(documentId -> {
                    module.document(documentId).syntaxTree().rootNode().accept(connectorNodeVisitor);
                });


                TypeDefinitionNode recordNode = null;
                JsonElement recordJson = null;

                for (Map.Entry<String, TypeDefinitionNode> recordEntry
                        : connectorNodeVisitor.getRecords().entrySet()) {
                    String key = recordEntry.getKey();
                    TypeDefinitionNode record = recordEntry.getValue();

                    JsonElement jsonST = DiagramUtil.getTypeDefinitionSyntaxJson(record, semanticModel);

                    if (record.typeName().text().equals(request.getName())) {
                        recordNode = record;
                        recordJson = jsonST;
                    } else {
                        recordDefJsonMap.put(key, jsonST);
                    }
                }

                Gson gson = new Gson();
                if (recordNode != null) {
                    if (recordJson instanceof JsonObject) {
                        JsonElement recordsJson = gson.toJsonTree(recordDefJsonMap);
                        ((JsonObject) recordJson).add("records", recordsJson);
                    }
                    recordCache.addRecordAST(request.getOrg(), request.getModule(),
                            request.getVersion(), request.getName(), recordJson);
                }

                ast = recordCache.getRecordAST(request.getOrg(), request.getModule(),
                        request.getVersion(), request.getName());
            } catch (Exception e) {
                String msg = "Operation 'ballerinaConnector/record' for " + cacheableKey + ":" +
                        request.getName() + " failed!";
                error = e.getMessage();
                logError(msg, e, null, (Position) null);
            }

        }
        BallerinaRecordResponse response = new BallerinaRecordResponse(request.getOrg(), request.getModule(),
                request.getVersion(), request.getName(), ast, error, request.getBeta());
        return CompletableFuture.supplyAsync(() -> response);
    }


    private String getCacheableKey(String orgName, String moduleName, String version) {
        return orgName + "_" + moduleName + "_" +
                (version.isEmpty() ? ProjectDirConstants.BLANG_PKG_DEFAULT_VERSION : version);
    }

    private BallerinaConnectorsResponse getConnectorConfig() throws IOException {
        try (InputStream inputStream = new FileInputStream(new File(connectorConfig));) {
            Toml toml;
            try {
                toml = new Toml().read(inputStream);
            } catch (IllegalStateException e) {
                throw new BLangCompilerException("invalid connector.toml due to " + e.getMessage());
            }
            return toml.to(BallerinaConnectorsResponse.class);
        }
    }
}
