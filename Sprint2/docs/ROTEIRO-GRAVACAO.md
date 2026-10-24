# Roteiro para gravação do build e da demonstração

## Preparação

- Use o IntelliJ para abrir `Sprint2` e abra o terminal integrado.
- Confirme que o serviço `MongoDB` está em execução.
- Use o emulador Android `Pixel_9`.
- Neste computador a porta 8080 pertence ao Apache. A demonstração usa a porta 18080.
- Feche notificações e nunca deixe a chave Gemini visível na gravação.

## 1. Mostrar o build do backend

No terminal do IntelliJ:

```powershell
cd "C:\Users\Aluno Temp\Downloads\Sprint2\Sprint2\backend"
$env:JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=C:\Users\Public\voaaguia-tmp -Djdk.net.unixdomain.tmpdir=C:\Users\Public\voaaguia-tmp"
.\mvnw.cmd clean package
```

Mostre no terminal:

- `Tests run: 8, Failures: 0, Errors: 0`;
- `BUILD SUCCESS`;
- o JAR criado em `backend\target\backend-2.0.0.jar`.

Narração sugerida: “O backend usa Java 17, Spring Boot, Spring Security com JWT, MongoDB para os dados de negócio e JPA/Hibernate com H2 para auditoria.”

## 2. Iniciar o backend com IA

Ainda no terminal do IntelliJ:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-demo.ps1
```

Cole a chave quando o terminal solicitar. A entrada fica oculta e a chave permanece somente no processo. Aguarde a mensagem:

```text
Started Application
```

Endereços usados:

- API e Swagger: `http://localhost:18080/swagger-ui/index.html`;
- Android no emulador: `http://10.0.2.2:18080/api/`.

Contas de demonstração, todas com a senha `VoaAguiaDemo2026!`:

| Perfil | E-mail |
|---|---|
| Líder | `lider@voaaguia.local` |
| Operador | `operador@voaaguia.local` |
| Gestor | `gestor@voaaguia.local` |

## 3. Mostrar o build do Android

Abra um segundo terminal do IntelliJ:

```powershell
cd "C:\Users\Aluno Temp\Downloads\Sprint2\Sprint2\android"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build-local.ps1 -ApiBaseUrl "http://10.0.2.2:18080/api/"
```

Mostre `BUILD SUCCESSFUL` e o APK criado em:

```text
android\app\build\outputs\apk\debug\app-debug.apk
```

Para instalar e abrir no emulador:

```powershell
$adb="C:\Users\Aluno Temp\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.voaaguia/.MainActivity
```

## 4. Fluxo funcional recomendado

1. Entre como **líder**.
2. Abra **Orientações Estratégicas** e cadastre uma estratégia com título, categoria e campanha.
3. Saia e entre como **operador**.
4. Abra **Ideias de Inovação** e cadastre uma ideia vinculada à estratégia.
5. Mostre que o operador pode consultar, editar e excluir a própria ideia enquanto ela está pendente.
6. Saia e entre como **gestor**.
7. Abra **Ideias de Inovação** e toque no ícone dourado de IA da ideia.
8. Mostre a análise Gemini com notas de impacto, viabilidade, alinhamento, riscos e prioridade sugerida.
9. Abra a avaliação da ideia, use **Sugerir Feedback (IA)** e aprove com prioridade alta.
10. Abra **Projetos**, crie um projeto a partir da ideia aprovada e atualize etapa, progresso, investimento e resultados.
11. Saia e entre como **líder**.
12. Abra **Projetos** para mostrar o acompanhamento.
13. Abra **Dashboard**, mostre ROI, lucro, investimento, produtividade e filtros.
14. Toque em **Gerar Insights com IA** e mostre as recomendações executivas.
15. Termine no **Ranking de Inovadores**.

Se o Gemini responder com indisponibilidade temporária, aguarde alguns segundos e toque novamente. A API externa pode devolver HTTP 503 mesmo com chave e integração válidas.

## 5. Pontos para narrar

- A chave Gemini fica apenas no backend e nunca é enviada ao aplicativo.
- O JWT controla a sessão e cada endpoint valida o perfil do usuário.
- O aplicativo não usa mocks: todas as telas consomem a API REST real.
- Estratégias e projetos são vinculados às ideias e à estratégia vigente.
- A IA auxilia a decisão do gestor e da liderança, mas não aprova ideias automaticamente.
- O dashboard calcula lucro e ROI a partir dos dados persistidos.

Para encerrar o backend, volte ao primeiro terminal e pressione `Ctrl+C`.
