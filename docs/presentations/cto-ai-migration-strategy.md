---
marp: true
theme: gaia
paginate: true
backgroundColor: #fff
header: "OneFinancial — Migracao Assistida por IA"
footer: "Confidencial"
style: |
  section {
    font-size: 24px;
  }
  section.lead h1 {
    font-size: 42px;
  }
  table {
    font-size: 20px;
    width: 100%;
  }
  .columns {
    display: flex;
    gap: 2rem;
  }
  .columns > div {
    flex: 1;
  }
  .poc-placeholder {
    background: #fff3cd;
    border: 2px dashed #ffc107;
    border-radius: 8px;
    padding: 12px;
    text-align: center;
    color: #856404;
    font-style: italic;
  }
  .metric-box {
    background: #e8f5e9;
    border-radius: 8px;
    padding: 16px;
    text-align: center;
    margin: 8px;
  }
  .metric-box h2 {
    margin: 0;
    color: #2e7d32;
  }
  .risk-high { color: #d32f2f; font-weight: bold; }
  .risk-med { color: #f57c00; font-weight: bold; }
  .risk-low { color: #388e3c; font-weight: bold; }
---

<!-- _class: lead -->
<!-- _paginate: false -->

# Migracao de Servicos Legados com IA

**Framework de Migracao Assistida — OneFinancial**

Arquitetura Hexagonal + Spring Modulith + Copilot Chat

Fevereiro 2026

<!-- Notas do apresentador:
Abrir com o contexto: temos dezenas de servicos legados Spring Boot que precisam
ser modernizados para arquitetura hexagonal. A proposta e acelerar isso com IA.
-->

---

<!-- _class: invert -->

# O Desafio

<div class="columns">
<div>

### Cenario Atual

- Dezenas de servicos Spring Boot legados
- Acoplamento forte entre dominio e infraestrutura
- Sem padronizacao entre equipes
- Testes insuficientes ou ausentes
- Migracao manual: lenta, cara, inconsistente

</div>
<div>

### O Que Queremos

- Arquitetura hexagonal padronizada
- Modulos independentes (Spring Modulith)
- Feature flags (secure-by-default)
- Testes automatizados (>80% cobertura)
- Processo reproduzivel entre equipes

</div>
</div>

<!-- Notas do apresentador:
Enfatizar que o problema nao e tecnico — sabemos COMO migrar.
O problema e ESCALA: fazer isso de forma consistente em dezenas de servicos.
-->

---

# Custo do Status Quo

| Risco | Impacto | Probabilidade | Consequencia |
|-------|---------|---------------|--------------|
| Divida tecnica acumulada | Alto | Certa | Custo exponencial de manutencao |
| Inconsistencia entre servicos | Alto | Alta | Onboarding lento, bugs cross-service |
| Vulnerabilidades nao detectadas | Critico | Media | Exposicao de dados, compliance |
| Perda de conhecimento | Medio | Alta | Dependencia de pessoas-chave |
| Tempo de migracao manual | Alto | Certa | 4-8 semanas por servico (estimativa) |

> **Cada mes sem migrar aumenta o custo futuro.**
> Servicos legados acumulam mais acoplamento, mais debito, mais risco.

<!-- Notas do apresentador:
Usar este slide para justificar a urgencia. O custo de NAO fazer nada e real e crescente.
-->

---

# Por Que Arquitetura Hexagonal?

```
+---------------------------------------------------------+
|                   Host Application                       |
|  +-------------+  +--------------+  +---------------+   |
|  | Validators  |  | Enrichers    |  | Event         |   |
|  | (SPI)       |  | (SPI)       |  | Listeners     |   |
|  +------+------+  +------+------+  +---------------+   |
+---------|--------------|---------------------------------+
|         v              v      Customer Registry Starter  |
|  +-------------------------------------------+          |
|  |       CustomerRegistryService             |          |
|  |  validate -> dedup -> enrich -> persist   |          |
|  +--------+-----------------------+----------+          |
|           v                       v                      |
|  +----------------+    +------------------+              |
|  | CustomerRepo   |    | EventPublisher   |              |
|  | (Port)         |    | (Port)           |              |
|  +-------+--------+    +--------+---------+              |
|          v                      v                        |
|  +----------------+    +------------------+              |
|  | JPA Adapter    |    | Spring Events    |              |
|  | (ou InMemory)  |    | Adapter          |              |
|  +----------------+    +------------------+              |
+---------------------------------------------------------+
```

<!-- Notas do apresentador:
O diagrama mostra o padrao que TODOS os servicos migrados devem seguir.
Core sem dependencias de infra. Adapters substituiveis. Host extende via SPI.
-->

---

# Complexidade por Tier

| Tier | Entidades | Adapters | Frontend | Esforco Estimado | Fases |
|------|-----------|----------|----------|------------------|-------|
| **Simple** | 1 | 1-2 (REST + Persistence) | Nao | 1-2 semanas | 1-4 |
| **Standard** | 2-5 | 3 (+ Events) | Opcional | 2-4 semanas | 1-4 |
| **Advanced** | 5+ | 4+ (+ Observability) | Sim | 4-8 semanas | 1-5 |

### Referencia: Customer Registry

- **Tier**: Advanced
- **6 modulos**: core, persistence, rest, events, observability, autoconfigure
- **331 testes** (222 Java + 109 Angular), zero falhas
- **5 endpoints** REST, 4 feature flags, 5 ferramentas de seguranca

<!-- Notas do apresentador:
A classificacao por tier ajuda a priorizar: comecamos pelas migracoes Simple
para validar o processo antes de atacar servicos Advanced.
-->

---

<!-- _class: invert -->

# A Solucao: Framework de Migracao OneFinancial

---

# Componentes do Framework

<div class="columns">
<div>

### Artefatos

- **20 prompts** prontos para Copilot Chat
  organizados por fase (0-5)
- **6 instruction files** para o VS Code
  (carregados automaticamente)
- **Scorecard** com 23 dimensoes
  automatizaveis por CI
- **Template** de Lessons Learned
  para feedback loop

</div>
<div>

### Processo

- **Workspace multi-root** (3 diretorios)
- **Fases sequenciais** com gates de qualidade
- **Recovery prompts** para erros comuns
- **Verificacao automatica** via CI

</div>
</div>

| Artefato | Arquivo |
|----------|---------|
| Prompts | `migration/copilot-prompts.md` |
| Scorecard | `migration/scorecard.md` |
| Instruction files | `migration/template/.github/instructions/` |
| Lessons Learned | `migration/template/MIGRATION-LESSONS.md.template` |

---

# Workspace Multi-Root com Copilot Chat

```
migration-workspace/
|
+-- reference/          <-- Repositorio OneFinancial (read-only)
|   +-- customer-registry-starter/
|   +-- migration/
|       +-- copilot-prompts.md
|       +-- scorecard.md
|       +-- template/
|
+-- legacy/             <-- Servico legado sendo migrado (read-only)
|   +-- src/main/java/...
|
+-- target/             <-- Codigo gerado pela migracao
|   +-- src/main/java/...
|   +-- .github/instructions/    <-- Copilot instruction files
|   +-- MIGRATION-LESSONS.md
|
+-- migration-workspace.code-workspace
```

**Copilot le o `reference/`, analisa o `legacy/`, gera codigo no `target/`.**

<!-- Notas do apresentador:
Este setup e o nucleo do workflow. O Copilot tem visibilidade dos tres diretorios
simultaneamente, permitindo o padrao "Ler referencia -> Analisar legado -> Gerar codigo".
-->

---

# Fases da Migracao (0 a 5)

| Fase | Nome | Peso Scorecard | Entregas Principais |
|------|------|---------------|---------------------|
| **0** | Analise do Legado | (pre-scorecard) | `LEGACY-ANALYSIS.md`, `MIGRATION-PLAN.md` |
| **1** | Scaffold | 25% | Estrutura de pacotes, marker class, testes ArchUnit |
| **2** | Core Extraction | 35% | Modelo de dominio puro, ports, services, unit tests |
| **3** | Adapters | 15% | Persistence, REST, Events + bridge configs |
| **4** | Auto-Configuration | 20% | Feature flags, `@ConditionalOnMissingBean`, META-INF |
| **5** | Frontend (opcional) | 5% | Angular library, standalone + OnPush + Signals |

> **Gate**: cada fase deve atingir 100% nas suas dimensoes antes de avancar.
> Servicos backend-only atingem 95% (Fase 5 e opcional).

<!-- Notas do apresentador:
Os pesos refletem a importancia relativa de cada fase.
Fase 2 (Core) tem o maior peso porque e onde esta o valor de negocio.
-->

---

# Prompt Engineering: Padrao "Ler -> Analisar -> Gerar"

### Estrutura de um Prompt Tipico

```
1. Ler as regras:
   #file:reference/migration/template/.github/instructions/core-domain.instructions.md

2. Ler o padrao de referencia:
   #file:reference/customer-registry-starter/.../Customer.java

3. Analisar o legado:
   #file:legacy/src/main/java/.../LegacyEntity.java

4. Gerar no target com regras explicitas:
   - PROIBIDO: import jakarta.persistence.*
   - OBRIGATORIO: static factories, PII masking, UUID
```

### Por que funciona?

- Copilot **ve o padrao correto** antes de gerar
- Regras **explicitas** reduzem desvios
- **Recovery prompts** para cada violacao conhecida (6 padroes documentados)

---

# Guardrails Automatizados

### 23 Dimensoes do Scorecard (verificaveis por CI)

<div class="columns">
<div>

**Fase 1 — Fundacao (25%)**
- Estrutura de pacotes hexagonal
- Marker class + Modulith test
- Pipeline CI com gates

**Fase 2 — Core (35%)**
- Isolamento do core (zero deps infra)
- Port interfaces definidas
- Cobertura de testes >= 80%

</div>
<div>

**Fase 3 — Adapters (15%)**
- Bridge configs em todos adapters
- Testes de integracao por adapter

**Fase 4 — Auto-Config (20%)**
- `@ConditionalOnMissingBean`
- Feature flags off-by-default
- META-INF + Context Runner tests

**Fase 5 — Frontend (5%)**
- Standalone + OnPush

</div>
</div>

> **Cada dimensao tem um comando CI concreto** para verificacao automatica.

---

# Feedback Loop: Melhoria Continua

```
  Migracao N
      |
      v
  MIGRATION-LESSONS.md     <-- Desenvolvedor preenche por fase
      |
      v
  Review identifica melhorias
      |
      +----> Atualiza copilot-prompts.md (20 prompts)
      |
      +----> Atualiza instruction files (6 arquivos)
      |
      +----> Adiciona novos recovery prompts
      |
      v
  Migracao N+1 se beneficia das melhorias
```

### Template Estruturado

- **Diario por fase**: acertos, desvios, gaps
- **Resumo**: o que funcionou, o que precisa melhorar
- **Sugestoes concretas**: "Prompt 3.1 deve mencionar `@JdbcTypeCode`"
- **Inventario de arquivos**: quais foram corretos na 1a tentativa

---

# Seguranca como Codigo

| Ferramenta | O Que Verifica | Threshold |
|------------|----------------|-----------|
| SpotBugs + FindSecBugs | SAST Java (bugs + seguranca) | Severidade Media |
| Semgrep | SAST baseado em padroes | Severidade WARNING |
| OWASP Dependency-Check | CVEs em dependencias Java | CVSS >= 7 |
| npm audit | CVEs em dependencias npm | Severidade High |
| CycloneDX | Geracao de SBOM (Java + Angular) | — |

### Principios de Design

- **Secure-by-default**: todas as features OFF ate habilitacao explicita
- **Zero credenciais hardcoded**: env vars ou secrets managers
- **Prefixo de tabelas** (`cr_`): evita colisao com host
- **Validacao de input**: checksum CPF/CNPJ, sanitizacao de formato
- **Zero PII em metricas**: IDs e documentos nunca em labels de alta cardinalidade

---

<!-- _class: invert -->

# Evidencias das POCs

---

# POC Overview

<!-- POC_DATA_START -->

| Servico | Tier | Fase Alcancada | Score Final | Tempo Total | Observacoes |
|---------|------|---------------|-------------|-------------|-------------|
| payment-gateway | Simple | 4 | 98% | 5.8h | Re-run com prompts melhorados. Melhoria significativa. (tentativa 2, 93% → 98%) |
| account-ledger | Standard | 5 | 89% | 18.5h | Servico multi-entidade. Fase 4 precisou 2 iteracoes. |

<!-- POC_DATA_END -->

<!-- Notas do apresentador:
Este slide sera atualizado com dados reais apos cada POC.
Por enquanto, serve para mostrar a estrutura de medicao ao CTO.
-->

---

# Resultados Quantitativos

<!-- POC_DATA_START -->

<div class="columns">
<div>

### Tempo

| Metrica | Valor |
|---------|-------|
| Tempo medio por fase | 0.7h / 1.0h / 4.0h / 3.0h / 2.5h / 2.0h |
| Tempo total (Simple) | 5.8h |
| Tempo total (Standard) | 18.5h |
| Tempo medio total | 12.2h |

</div>
<div>

### Qualidade

| Metrica | Valor |
|---------|-------|
| Codigo correto na 1a tentativa | 76% |
| Recovery prompts usados (media) | 3.0 |
| Violacoes ArchUnit pos-geracao (media) | 2.0 |
| Score final medio | 94% |

</div>
</div>

<!-- POC_DATA_END -->

---

# Scorecard Comparativo

<!-- POC_DATA_START -->

| Fase | Peso | payment-gateway | account-ledger | Meta |
|------|------|----|----|------|
| 1 — Fundacao | 25% | 100% | 100% | 100% |
| 2 — Core | 35% | 100% | 90% | 100% |
| 3 — Adapters | 15% | 95% | 85% | 100% |
| 4 — Auto-Config | 20% | 95% | 80% | 100% |
| 5 — Frontend | 5% | N/A | 75% | N/A |
| **Total** | **100%** | 98% | 89% | **95%+** |

<!-- POC_DATA_END -->

---

# Erros Comuns do Copilot e Taxa de Recuperacao

<!-- POC_DATA_START -->

| Violacao | Fase | Frequencia | Recovery Prompt | Taxa de Correcao |
|----------|------|-----------|-----------------|------------------|
| JPA annotations no core | 2 | 5x (account-ledger, payment-gateway) | Prompt 2.1 recovery | 5/5 |
| Controller publico | 3 | 3x (account-ledger, payment-gateway) | Prompt 3.2 recovery | 3/3 |
| Mapper na entity em vez de classe separada | 3 | 2x (account-ledger) | Prompt 3.1 recovery | 0/2 |
| matchIfMissing = true | 4 | 1x (payment-gateway) | Prompt 4.1 recovery | 1/1 |
| @ComponentScan no auto-config | 4 | 1x (account-ledger) | Prompt 4.1 recovery | 1/1 |

<!-- POC_DATA_END -->

---

# Feedback Loop em Acao

<!-- POC_DATA_START -->

### Melhorias Derivadas das POCs

| POC | Artefato Modificado | Melhoria | Impacto |
|-----|--------------------|-----------| --------|
| payment-gateway (#1) | copilot-prompts.md | Prompt 2.1 agora menciona @JdbcTypeCode | Reducao de violacoes JSONB em Fase 3 |
| account-ledger (#1) | copilot-prompts.md | Prompt 3.1 agora enfatiza CustomerEntityMapper separado | Previne mapper methods na entity JPA |
| account-ledger (#1) | core-domain.instructions.md | Adicionado Persistable<UUID> ao checklist | Previne SELECT extra em INSERT |

<!-- POC_DATA_END -->

---

# Licoes Aprendidas Consolidadas

<!-- POC_DATA_START -->

<div class="columns">
<div>

### O Que Funcionou Bem
- Scaffold (Fase 1) correto em 100% dos casos (payment-gateway, account-ledger)
- Recovery prompts resolveram 85%+ das violacoes (payment-gateway)
- Workspace multi-root permite Copilot comparar referencia e legado (payment-gateway, account-ledger)

</div>
<div>

### O Que Precisa Melhorar
- Fase 4 (auto-config) requer 2+ iteracoes (account-ledger)
- Copilot confunde mapper com entity em multi-entidade (account-ledger)
- Prompts de Fase 2 nao mencionam PII masking explicitamente (payment-gateway)

</div>
</div>

<!-- POC_DATA_END -->

---

<!-- _class: invert -->

# Projecao e Decisao

---

# ROI Projetado

### Modelo de Calculo

```
Custo Manual por Servico = Tempo(tier) x CustoHora x NumDevs
Custo Assistido por Servico = Tempo(tier) x FatorReducao x CustoHora x NumDevs

Economia por Servico = Custo Manual - Custo Assistido
ROI Total = Economia x NumServicos - CustoFramework
```

### Variaveis (ajustar com dados reais das POCs)

| Variavel | Estimativa Conservadora | Estimativa Otimista |
|----------|------------------------|---------------------|
| Fator de reducao de tempo | 30% | 50% |
| Num. servicos a migrar | <!-- AJUSTAR --> | <!-- AJUSTAR --> |
| Custo/hora medio (R$) | <!-- AJUSTAR --> | <!-- AJUSTAR --> |
| Custo do framework (unico) | Ja investido | Ja investido |

> O framework ja esta construido. O custo incremental e apenas o tempo de cada POC.

---

# Roadmap de Adocao

```
Trimestre    Fase            Entregaveis
------------ --------------- ----------------------------------------
T1 2026      POC             2-3 servicos Simple migrados
             (atual)         Scorecard validado por CI
                             Primeiras MIGRATION-LESSONS.md

T2 2026      Piloto          5-8 servicos (mix Simple + Standard)
                             Treinamento de 2 equipes
                             Refinamento de prompts com feedback

T3 2026      Escala          Todas as equipes treinadas
                             Pipeline CI com migration-check.yml
                             Meta: 15-20 servicos migrados

T4 2026      Maturidade      Servicos Advanced
                             Automacao de scorecard no dashboard
                             Framework como produto interno
```

<!-- Notas do apresentador:
Este roadmap e conservador. Se as POCs forem positivas, podemos acelerar.
O framework ja existe, entao o gargalo e treinamento e disponibilidade das equipes.
-->

---

# Pre-Requisitos por Equipe

### Ferramentas

| Ferramenta | Custo | Observacao |
|------------|-------|------------|
| VS Code | Gratuito | IDE padrao |
| GitHub Copilot Business | Licenca/dev/mes | Copilot Chat com `#file:` e `@workspace` |
| Docker Desktop | Gratuito (ou licenca) | Build environment obrigatorio |
| Git 2.x+ | Gratuito | Controle de versao |

### Conhecimento

- Conceitos de arquitetura hexagonal (1h de estudo)
- Spring Modulith module boundaries (1h de estudo)
- Workflow do framework (documentado em `docs/copilot-migration-strategy.md`)

### Setup Time

- **30 minutos**: workspace multi-root + instruction files configurados
- **Sem instalacao local** de Java, Maven, Node ou npm

---

# Riscos e Mitigacoes

| Risco | Prob. | Impacto | Mitigacao |
|-------|-------|---------|-----------|
| Qualidade do codigo gerado pela IA | Media | Alto | 23 verificacoes automaticas no scorecard + ArchUnit |
| Resistencia das equipes | Media | Medio | Comece com voluntarios; use POCs como evidencia |
| Curva de aprendizado do framework | Baixa | Medio | 6 instruction files + 20 prompts prontos + troubleshooting |
| Evolucao do Copilot quebra prompts | Baixa | Medio | Feedback loop: `MIGRATION-LESSONS.md` detecta regressoes |
| Servicos com padroes nao cobertos | Media | Medio | Tier Advanced tem fases adicionais; novos prompts criados por POC |
| Dependencia de licenca Copilot | Baixa | Baixo | Framework funciona com qualquer LLM que suporte chat |

<!-- Notas do apresentador:
O risco #1 (qualidade IA) e o que mais preocupa CTOs. A resposta e que NAO
confiamos cegamente: temos 23 verificacoes automaticas + ArchUnit + ModulithTest.
-->

---

# Metricas de Sucesso (KPIs)

| KPI | Alvo T1 (POC) | Alvo T2 (Piloto) | Alvo T4 (Escala) |
|-----|---------------|------------------|-------------------|
| Servicos migrados | 2-3 | 8-10 | 20+ |
| Score medio no scorecard | >= 95% | >= 95% | >= 95% |
| Codigo correto na 1a tentativa | >= 70% | >= 80% | >= 85% |
| Reducao de tempo vs. manual | >= 30% | >= 40% | >= 50% |
| Equipes treinadas | 1 | 3 | Todas |
| Satisfacao do desenvolvedor | Feedback qualitativo | NPS >= 7 | NPS >= 8 |

> **Scorecard automatizado no CI** e a metrica mais objetiva.
> As demais sao derivadas dos `MIGRATION-LESSONS.md`.

---

# Proximos Passos

| Acao | Responsavel | Prazo | Dependencia |
|------|-------------|-------|-------------|
| Aprovar licencas Copilot Business | CTO / Compras | <!-- AJUSTAR --> | Decisao deste meeting |
| Selecionar 2-3 servicos Simple para POC | Tech Leads | 1 semana apos aprovacao | Lista de servicos candidatos |
| Executar POC 1 com framework completo | Equipe voluntaria | 2 semanas | Licencas + workspace configurado |
| Revisar MIGRATION-LESSONS.md da POC 1 | Arquitetura | 1 dia apos conclusao | POC 1 finalizada |
| Ajustar prompts/instructions com feedback | Equipe do framework | 3 dias | Review concluida |
| Executar POCs 2 e 3 | Equipes voluntarias | 2 semanas cada | Prompts atualizados |
| Apresentar resultados das POCs | Tech Lead | 1 semana apos POCs | POCs finalizadas |
| Decisao de escala (go/no-go) | CTO | Fim do T1 | Resultados das POCs |

---

<!-- _class: lead -->

# Perguntas?

<br>

**Framework de Migracao OneFinancial**
Repositorio: `onefinancial/customer-registry`

Documentacao:
- `docs/copilot-migration-strategy.md` — Workflow completo
- `migration/copilot-prompts.md` — 20 prompts prontos
- `migration/scorecard.md` — 23 dimensoes automatizaveis

<!-- Notas do apresentador:
Abrir para perguntas. Ter o repositorio aberto no VS Code para demonstracoes
ao vivo se necessario. Mostrar o workspace multi-root em acao.
-->
