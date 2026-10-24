# Contrato REST - VoaÁguia Sprint 2

Base: `/api`. JSON UTF-8. Todas as rotas, exceto login e cadastro, exigem `Authorization: Bearer <token>`. Datas usam ISO-8601 UTC, por exemplo `2026-09-20T12:00:00Z`. Valores monetários são números em reais. O contrato OpenAPI exportado detalha os schemas; com a API em execução, consulte `/v3/api-docs` e `/swagger-ui/index.html`.

## Autenticação

| Método / rota | Payload | Resposta |
|---|---|---|
| POST /auth/login | `{ "email":"gestor@voaaguia.local", "password":"sua-senha" }` | 200 `{token, expiresAt, user}` |
| POST /auth/register | `{ "email":"nome@exemplo.com", "password":"senha-com-8-ou-mais", "name":"Nome", "department":"Operação" }` | 201 `{token, expiresAt, user}`; role sempre OPERADOR |
| GET /auth/me | Sem corpo | 200 usuário com id, name, email, role, department e pontuação; sem hash |
| POST /auth/logout | `{}` | 204 sem corpo; JWT revogado |

## Estratégias

Todos podem consultar; somente LIDER pode alterar.

| Método / rota | Payload | Resposta |
|---|---|---|
| GET /strategies | Sem corpo | 200 lista de estratégias ativas |
| GET /strategies/{id} | Sem corpo | 200 estratégia, inclusive desativada |
| GET /strategies/{id}/history | Sem corpo | 200 lista `{id,date,category,campaign,title,description,isActive,changedBy}` |
| POST /strategies | StrategyInput abaixo | 201 estratégia criada |
| PUT /strategies/{id} | StrategyInput abaixo | 200 estratégia atualizada; nova revisão |
| DELETE /strategies/{id} | Sem corpo | 204; desativação e registro histórico |

```json
{"title":"Eficiência nas rotas","description":"Reduzir desperdícios de combustível","category":"Logística","campaign":"Eficiência 2026","isActive":true}
```

Resposta de estratégia: campos do payload + `id`, `authorId`, `authorName`, `createdAt`, `updatedAt`, `version`. IDs e autoria são atribuídos pelo servidor. O histórico só é exposto pela rota específica.

## Ideias

Operadores só veem as próprias ideias. Gestores e líderes podem consultá-las. Somente OPERADOR cria/edita/exclui; edição e exclusão exigem status PENDENTE. Somente GESTOR avalia.

| Método / rota | Payload | Resposta |
|---|---|---|
| GET /ideas | Sem corpo | 200 lista autorizada |
| GET /ideas/{id} | Sem corpo | 200 ideia autorizada |
| POST /ideas | IdeaInput abaixo | 201 ideia criada |
| PUT /ideas/{id} | IdeaInput abaixo | 200 ideia atualizada |
| DELETE /ideas/{id} | Sem corpo | 204; exclusão lógica |
| PATCH /ideas/{id}/review | ReviewInput abaixo | 200 ideia com decisão |
| POST /ideas/{id}/vote | `{}` | 200 ideia com votes; um voto por pessoa |

IdeaInput:

```json
{"title":"Rotas mais eficientes","description":"Agrupar entregas por proximidade","category":"Logística","strategyId":"id-da-estrategia"}
```

ReviewInput:

```json
{"status":"APROVADA","priority":"ALTA","feedback":"Aderente à campanha e viável para piloto"}
```

Status de avaliação: EM_ANALISE, APROVADA, REJEITADA. Prioridade: BAIXA, MEDIA, ALTA, CRITICA. EM_PROJETO é derivado do vínculo persistido, não enviado na avaliação.

Resposta de ideia: payload + `id`, `authorId`, `authorName`, `department`, `status`, `priority`, `feedback`, `votes`, `createdAt`, `updatedAt`, `version`. Votantes e campos internos não são expostos.

## Projetos

GESTOR faz CRUD; LIDER consulta. OPERADOR não acessa esses endpoints. Criação exige ideia aprovada e estratégia vigente igual à da ideia. Vínculos não podem ser alterados em PUT.

| Método / rota | Payload | Resposta |
|---|---|---|
| GET /projects | Sem corpo | 200 lista não excluída |
| GET /projects/{id} | Sem corpo | 200 projeto |
| POST /projects | ProjectInput abaixo | 201 projeto criado |
| PUT /projects/{id} | ProjectInput abaixo (objeto completo) | 200 projeto atualizado |
| DELETE /projects/{id} | Sem corpo | 204; exclusão lógica |

```json
{"title":"Piloto de rotas","description":"Validar o agrupamento por região","ideaId":"id-da-ideia","strategyId":"id-da-estrategia","status":"EM_ANDAMENTO","stage":"PILOTO","startDate":"2026-09-20T00:00:00Z","expectedEndDate":"2026-12-20T00:00:00Z","actualEndDate":null,"investment":1000,"expectedReturn":1800,"actualReturn":1500,"costReduction":200,"productivityGain":10,"progressPercent":50,"notes":"Primeiro resultado"}
```

Status: PLANEJAMENTO, EM_ANDAMENTO, CONCLUIDO, CANCELADO. Etapa: IDEACAO, VALIDACAO, DESENVOLVIMENTO, PILOTO, IMPLANTACAO, OPERACAO. Progresso e produtividade entre 0 e 100; valores financeiros não negativos; prazo não anterior ao início. CONCLUIDO exige progresso 100 e actualEndDate.

Resposta de projeto: payload + `id`, `managerId`, `managerName`, `createdAt`, `updatedAt`, `version`. O servidor define autoria e identificador.

## Relatórios e ranking

| Método / rota | Perfil | Payload / resposta |
|---|---|---|
| GET /reports/dashboard?strategyId={id}&projectId={id} | LIDER | Sem corpo; ambos os filtros opcionais. 200 resumo abaixo |
| GET /ranking | Todos | Sem corpo; 200 até 10 usuários `{id,name,department,points,ideasCount,approvedIdeasCount}` |

Resumo: `{totalInvestment,totalReturn,profit,roi,roiAvailable,totalCostReduction,avgProductivityGain,activeProjects,completedProjects,overdueProjects,totalIdeas,approvedIdeas,projects}`. ROI pode ser nulo quando o investimento é zero. Projetos excluídos não entram nos cálculos. O ranking atribui 10 pontos por ideia não excluída e 50 por ideia aprovada ao menos uma vez.

## IA via Gemini

POST com `{}` em todas as rotas. Resposta 200: `{ "text":"resposta real do modelo", "model":"modelo configurado", "provider":"Google Gemini" }`.

| Rota | Perfil | Função |
|---|---|---|
| /ai/ideas/{id}/analysis | GESTOR | Pontuação e prioridade sugerida com contexto estratégico |
| /ai/ideas/{id}/project-suggestion | GESTOR | Objetivo, etapas, prazo e KPIs sugeridos |
| /ai/ideas/{id}/feedback?approving=true | GESTOR | Sugestão de feedback para aprovação ou rejeição |
| /ai/dashboard/insights?strategyId={id}&projectId={id} | LIDER | Insights a partir dos dados reais resumidos; filtros opcionais |

## Erros

400: campo inválido/JSON malformado. 401: credenciais ou JWT inválidos, expirados/revogados. 403: perfil não autorizado. 404: registro ausente ou ideia de outro operador. 409: conflito de regra, duplicidade ou alteração concorrente. 502: falha da IA externa. 503: IA sem chave configurada. Erros de negócio usam `{ "message":"descrição" }`; validações incluem `errors`. As respostas de segurança 401/403 também podem ser sem corpo.
