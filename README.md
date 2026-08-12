<div align="center">

# AlkaCore

### A infraestrutura compartilhada da rede Alka*

Banco de dados, economia, mensagens, GUI e agendamento assíncrono — **uma única**
implementação, usada por todos os plugins `Alka*`.

`Java 21` · `Paper 1.21.8+` · `Folia-ready` · `MySQL` & `SQLite`

</div>

---

## O que é

O **AlkaCore** é o hub central da sua rede de plugins `Alka*`. Em vez de cada
plugin reinventar a roda (sua própria conexão de banco, sua própria GUI, suas
próprias mensagens), todos compartilham o mesmo núcleo: **uma** conexão de banco,
**um** motor de GUI, **uma** ponte de economia e **um** sistema de mensagens.

> Regra de ouro: **o Core não tem lógica de jogo.** Ele entrega a infraestrutura;
> o jogo fica nos plugins filhos (`AlkaEconomy`, `AlkaMines`, `AlkaRankUp`, ...).

## Recursos

| Módulo | Descrição |
| --- | --- |
| 🗄️ **DatabaseProvider** | Pool de conexões **HikariCP** robusto. MySQL com fallback automático para SQLite se o servidor de banco não responder. |
| 🧱 **AbstractRepository** | Base de repositórios com upsert genérico (`ON CONFLICT`/`ON DUPLICATE KEY`) e helper de **transação** para operações atômicas multi-linha. |
| 🗂️ **SchemaMigrator** | Versionamento de schema com migrações transacionais — cresça a rede sem apagar o banco. |
| 💰 **CurrencyAPI** | Moedas secundárias genéricas (pó, fragmentos, tokens) numa tabela única, com variantes **assíncronas** (`CompletableFuture`). |
| 💵 **EconomyBridge** | Ponte para a economia principal via **Vault** (funciona com qualquer provider, ex.: AlkaEconomy). |
| 💬 **MessageProvider** | Mensagens com **MiniMessage** e prefixo configurável. |
| 🖼️ **BaseGui** | Motor de inventários (páginas, itens com ação, skulls) usado por todos os plugins. |
| 🔌 **Hooks** | Detecção segura de `LuckPerms`, `PlaceholderAPI` e `ProtocolLib` — se um não carregar, o Core não quebra. |
| ⚡ **AlkaScheduler** | Agendamento assíncrono via `AsyncScheduler` do Paper — funciona **igual no Folia**. Nenhum I/O de banco na main thread. |

## Instalação

1. Coloque `AlkaCore.jar` na pasta `plugins/` do servidor (Paper **1.21.8+**).
2. Reinicie o servidor.
3. Opcionalmente, edite `plugins/AlkaCore/config.yml` para apontar para o seu MySQL.

### Dependências opcionais

| Dependência | Uso |
| --- | --- |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Ponte de economia (se ausente, economia simplesmente não está disponível). |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Parsing de placeholders. |
| [LuckPerms](https://luckperms.net/) | Grupo primário do jogador. |
| [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) | Detecção de presença (packets ficam a cargo de cada plugin). |

## Configuração

```yaml
mysql:
  enabled: false      # true = tenta MySQL; se falhar, cai pra SQLite sozinho
  host: "localhost"
  port: 3306
  database: "alkacore"
  username: "root"
  password: "senha"
  pool-size: 10       # tamanho do pool HikariCP (MySQL)

sqlite:
  file: "database.db"

messages:
  prefix: "<gradient:#f6d365:#fda085>[AlkaCore]</gradient> <white>"
```

## Uso em um plugin `Alka*`

### 1. Dependa do AlkaCore

No `build.gradle.kts` do seu plugin, consuma a API compilada contra o Core
(`compileOnly`) — publicada via `mavenLocal`:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    compileOnly("com.alkacode:AlkaCore:1.0.1")
}
```

E declare a dependência no `plugin.yml` para garantir a ordem de carregamento:

```yaml
depend: [AlkaCore]
```

### 2. Estenda `AlkaPlugin`

```java
public class MeuPlugin extends AlkaPlugin {

    @Override
    protected void onPluginEnable() {
        // A API já está pronta e registrada pelo Core
        CurrencyAPI moedas = getAlkaAPI().getCurrency();
        moedas.addAsync(uuid, "meuplugin_fragmentos", 10); // assíncrono, não trava a main thread
    }

    @Override
    protected void onPluginDisable() {
        // cleanup
    }
}
```

### Trabalho assíncrono de banco

Nunca bloqueie a main thread. Para leituras/escritas que você precisa esperar, use
as variantes `*Async`:

```java
getAlkaAPI().getCurrency().getBalanceAsync(uuid, "alkamines_fragments")
    .thenAccept(saldo -> Bukkit.getScheduler().runTask(plugin, () -> {
        // volta pra main thread e usa o valor
    }));
```

E para tarefas periódicas (ex.: autosave), use o `AlkaScheduler`:

```java
getAlkaAPI().getScheduler()
    .runAsyncRepeating(() -> flush(), 20L, 6000L); // a cada 5 min, fora da main thread
```

## Compilar

```bash
./gradlew build
```

O jar final (com as libs de banco relocadas) fica em `build/libs/AlkaCore-1.0.1.jar`.
O jar "puro" da API é publicado no Maven local para os plugins filhos consumirem.

## Stack

- **Java 21** · **Gradle** (com `shadow` para embutir as libs de banco)
- **Paper API 1.21.8** (compatível com Folia)
- **HikariCP** (pool) · **SQLite JDBC** · **MySQL Connector/J**
- **Adventure/MiniMessage** para mensagens e GUI

## Licença

Projeto privado da rede **Alka\***. Uso interno.
