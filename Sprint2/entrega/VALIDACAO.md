# Validação técnica — 19/10/2026

## Backend

- Java 17, Spring Boot 3.5.6, MongoDB local e H2/JPA.
- `mvnw test`: 8 testes executados, sem falhas ou erros.
- Os testes usam servidor HTTP real e banco MongoDB isolado.
- Foram validados autenticação, autorização por perfil, CRUD de estratégias, histórico, CRUD de ideias, aprovação, votos, projetos, dashboard, ranking, logout e auditoria.

## Android

- Build `assembleDebug` concluído com sucesso para `http://10.0.2.2:18080/api/`.
- Teste instrumentado executado no emulador Pixel 9 com Android 16 contra backend e MongoDB reais.
- O teste percorreu líder, operador e gestor, incluindo histórico tipado, criação de ideia, aprovação, projeto, dashboard, ranking e encerramento da sessão.

## Correções de interface

- O cadastro de ideias seleciona automaticamente uma estratégia vigente.
- Falhas ao buscar estratégias são exibidas e podem ser tentadas novamente.
- O formulário permanece aberto quando a API rejeita o cadastro ou a edição.
- O histórico de estratégias deixou de usar parsing manual e agora apresenta carregamento, falha, lista vazia e versões estruturadas.
- Formulários de projeto permanecem abertos em caso de falha da API.

## Dados de apresentação

Com `SEED_ENABLED=true` e `SEED_SAMPLE_DATA=true`, o backend grava no MongoDB:

- duas estratégias, incluindo uma com duas versões históricas;
- três ideias em situações pendente, aprovada e vinculada a projeto;
- dois projetos, um em andamento e outro concluído;
- investimento total de R$ 80.000, retorno de R$ 115.000, ROI de 43,75%, redução de custos e ganho médio de produtividade de 18,5%.

Os registros são idempotentes e usam os mesmos endpoints e regras do aplicativo. Não são mocks do cliente.
