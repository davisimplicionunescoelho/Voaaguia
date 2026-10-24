# VoaÁguia - Sprint 2

Evolução do aplicativo Android da Sprint 1 para backend Java com Spring Boot, segurança JWT, MongoDB real e auditoria JPA/Hibernate.

- `backend/`: código Java, testes de integração, Maven Wrapper, Docker Compose e instruções.
- `android/`: código Kotlin integrado, Gradle Wrapper e instruções de compilação.
- `docs/`: arquitetura, apresentação e contrato de endpoints.

Comece pelo README do backend, execute a API e então instale o novo APK ou compile o app. A entrega da Sprint 1, localizada fora desta pasta, foi preservada.

Equipe da documentação original: Davi Simplicio Nunes Coelho (564403), Guilherme Vinicius Gonçalves da Silva (564401), Gustavo Okamoto Ventura (564945), Mirella Ayumi Miyakawa (564426).

Validação realizada em 20/09/2026: sete testes do backend com HTTP e MongoDB reais; um teste instrumentado em emulador Android 16 usando o cliente nativo para autenticar os três perfis e executar o fluxo estratégia/ideia/projeto/dashboard; compilação do APK e verificação da assinatura. Detalhes em `docs/VALIDACAO.md`.

Para gravar o build e a demonstração completa, incluindo os fluxos por perfil e a integração com Gemini, consulte `docs/ROTEIRO-GRAVACAO.md`.

Os arquivos preparados para entrega ficam em `entrega/`. Para reconstruir os pacotes depois de recompilar o app, execute `python empacotar.py` nesta pasta. O empacotador exclui `.env`, `local.properties`, caches e diretórios de build.

Pendências de validação externa: demonstrar uma resposta real do Gemini com a chave configurada e conferir se equipe e prazo permanecem os mesmos. Não reutilize uma chave exposta.
