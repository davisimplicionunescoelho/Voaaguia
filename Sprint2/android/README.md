# VoaÁguia Android - Sprint 2

Aplicativo nativo Kotlin/Jetpack Compose, adaptado da Sprint 1 e integrado exclusivamente à API REST da Sprint 2. As telas e identidade visual originais foram preservadas; as integrações diretas com Firebase e Gemini foram removidas.

## Compilar

Use Java 17, Android SDK 34 e Android Studio. Abra esta pasta e deixe o Gradle sincronizar. Configure `local.properties` com `sdk.dir` para seu SDK (o arquivo é local e não integra a entrega).

Windows: `./build-local.ps1`. Outros sistemas: `./gradlew assembleDebug`.

APK gerado: `app/build/outputs/apk/debug/app-debug.apk`.

## Endereço da API

O build padrão usa `http://10.0.2.2:8080/api/`, endereço do computador visto pelo emulador Android. Inicie o backend antes de usar o app.

Dispositivo físico: conecte computador e celular à mesma rede, libere a porta 8080 e compile com `./build-local.ps1 -ApiBaseUrl 'http://IP-DO-COMPUTADOR:8080/api/'`. Em Linux/macOS: `./gradlew assembleDebug -PAPI_BASE_URL=http://IP-DO-COMPUTADOR:8080/api/`.

Alternativa via USB: `adb reverse tcp:8080 tcp:8080` e compile com `http://127.0.0.1:8080/api/`.

HTTP é permitido somente na variante debug. Para uma publicação, use endpoint HTTPS e assinatura própria da equipe. O APK acadêmico é debug e não deve ser apresentado como um app publicado em loja.

## Sessão e fluxos

JWT somente em memória; o usuário entra novamente após encerramento do processo. A expiração retorna ao login. Novas contas públicas são sempre de operador. Gestores e líderes são provisionados pelo seed controlado do backend. As regras de acesso também são aplicadas pelo servidor.

Líder cadastra estratégia e campanha → operador cadastra ideia vinculada → gestor avalia e aprova → gestor cria projeto com prazo e registra progresso/resultados → líder consulta dashboard com filtros e gráficos. A IA é acionada pela API, nunca diretamente pelo celular.

## Teste instrumentado

`app/src/androidTest/.../ApiIntegrationTest.kt` executa o cliente Android contra backend e MongoDB reais. Inicie uma instância separada da API em 18081 com `MONGODB_URI` apontando para uma base exclusiva de testes e seed habilitado. Execute `gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.demoPassword=SENHA_DO_SEED`. Não reutilize uma senha pessoal. O teste cria registros nessa base; não utilize a base de produção. A URL padrão do teste é `http://10.0.2.2:18081/api/`, alterável pelo argumento `apiUrl` do runner.
