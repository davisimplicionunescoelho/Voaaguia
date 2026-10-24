# VoaÁguia - Backend Sprint 2

Java 17, Spring Boot 3.5.6, Spring Security, JWT, BCrypt, Spring Data MongoDB e JPA/Hibernate. MongoDB persiste os dados de negócio; H2/Hibernate registra auditoria de cadastro e login durante a execução da API. Não há Firebase nem dados mockados no fluxo da Sprint 2.

## Executar com Docker

1. Copie `.env.example` para `.env` e preencha `JWT_SECRET` com um segredo aleatório de pelo menos 32 bytes.
2. Para a demonstração, mantenha `SEED_ENABLED=true` e defina `SEED_PASSWORD` com pelo menos 12 caracteres.
   Use também `SEED_SAMPLE_DATA=true` para carregar estratégias, ideias, projetos e indicadores persistidos no MongoDB.
3. Execute `docker compose up --build` nesta pasta.
4. Abra `http://localhost:8080/swagger-ui/index.html`. O contrato JSON está em `/v3/api-docs`.

O banco persiste no volume `mongo_data`. Não use `docker compose down -v` se quiser conservar os dados.

## Executar sem Docker

Requisitos: Java 17 e MongoDB rodando em `localhost:27017`. O Maven Wrapper baixa o Maven na primeira execução.

No PowerShell, configure `.env` e execute `./run-local.ps1`. Alternativamente, exporte as variáveis de `.env.example` no shell e execute `./mvnw spring-boot:run` (Linux/macOS) ou `mvnw.cmd spring-boot:run` (Windows). O Spring não lê `.env` automaticamente: quem o carrega é o script PowerShell ou Docker Compose.

Se a porta 8080 estiver ocupada, adicione `PORT=18080` ao `.env` local e compile o Android com `-ApiBaseUrl 'http://10.0.2.2:18080/api/'`. Neste computador havia um serviço Apache em 8080; ele foi preservado e os testes da API usaram portas alternativas.

## Contas para demonstração

O seed é opt-in e cria somente os usuários ausentes:

| Perfil | E-mail |
|---|---|
| Operador | operador@voaaguia.local |
| Gestor | gestor@voaaguia.local |
| Líder | lider@voaaguia.local |

A senha é o valor de `SEED_PASSWORD`. Nenhuma senha fixa é distribuída. Alterar essa variável não troca a senha de usuários que já existem. O cadastro público sempre cria OPERADOR e ignora qualquer role enviada pelo cliente. O seed não deve ser ativado em uma implantação pública.

Quando `SEED_SAMPLE_DATA=true`, o backend também cria um conjunto idempotente de dados para apresentação: duas estratégias com histórico, três ideias em diferentes etapas e dois projetos com investimentos, retornos, produtividade e redução de custos. Esses registros são gravados no MongoDB e consumidos pela mesma API REST do aplicativo; não são mocks do cliente.

## IA real

Configure `GEMINI_API_KEY` no servidor e `GEMINI_MODEL` com um modelo disponível na sua conta. A sugestão inicial é `gemini-2.5-flash`, mas foi necessária a atualização para o modelo `gemini-3.6-flash` em decorrência de um 404 ao requisitar o modelo defasado indicado; disponibilidade e cota dependem do provedor. O backend chama a API REST oficial, nunca envia a chave ao Android e não substitui falhas por respostas fictícias. Sem chave: HTTP 503; falha externa: HTTP 502. Nenhuma aprovação acontece automaticamente. O texto da ideia e sua estratégia são enviados ao Google para análise quando o gestor aciona a função.

## Organização

- `api`: endpoints, contratos de entrada validados e respostas de erro.
- `service`: autenticação, regras de negócio, relatórios e chamada à IA.
- `domain`: documentos de usuários, estratégias, ideias, projetos e revisões.
- `repository`: acesso aos dados de negócio no MongoDB.
- `audit`: entidade, repositório e serviço JPA/Hibernate para auditoria de cadastro e login em H2.
- `security`: configuração JWT, permissões por método, índices e seed.

O JWT dura 60 minutos. Logout registra o identificador do token em uma coleção com TTL. O cliente mantém o token somente em memória. Reiniciar o aplicativo exige novo login. Operadores só acessam suas ideias; alterações e exclusões são permitidas enquanto pendentes. A estratégia é obrigatória e deve estar ativa no cadastro. Projetos exigem uma ideia aprovada da mesma estratégia. Uma ideia só pode originar um projeto, mesmo após sua exclusão lógica. O status EM_PROJETO é derivado do vínculo persistido. Estratégias guardam o histórico no mesmo documento, preservando versões em atualizações atômicas. Pontos são calculados pelos registros e não incrementados repetidamente na aprovação. Votos são únicos por usuário e ideia.

## Indicadores

Lucro = retorno realizado - investimento. ROI = lucro / investimento × 100. Quando o investimento é zero, ROI é `null` e `roiAvailable=false`; o app mostra N/A. Redução de custos é informada separadamente, sem somá-la novamente ao retorno. Produtividade é a média simples dos percentuais informados, não uma média ponderada. Os totais incluem projetos não excluídos, inclusive cancelados, pois podem conter custos e retornos efetivos. Os filtros `strategyId` e `projectId` restringem os dados antes do cálculo.

## Testar

Com MongoDB local em execução: `./mvnw test`. Os testes usam HTTP real e banco isolado `voaaguia_test_<uuid>`, removido ao final; não alteram a base de demonstração. No Windows com falha de socket temporário, use as opções de JVM de `run-local.ps1` antes de executar os testes.

Para compilar: `./mvnw package`. Resultado: `target/backend-2.0.0.jar`.

## Persistência

MongoDB é o banco principal para usuários, estratégias, ideias e projetos. A aplicação também usa JPA/Hibernate com H2 para persistir a trilha de auditoria de cadastro e login. Essa separação mantém os dados de negócio no NoSQL solicitado e atende ao requisito de JPA/Hibernate sem duplicar entidades de domínio.

Referências: https://docs.spring.io/spring-data/mongodb/reference/index.html, https://docs.spring.io/spring-data/jpa/reference/ e https://ai.google.dev/api/generate-content.
enerate-content.
