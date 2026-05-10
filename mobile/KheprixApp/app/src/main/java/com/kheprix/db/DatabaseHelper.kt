package com.kheprix.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database schema for offline study data.
 *
 * Every table has:
 *   - local_id      : local autoincrement PK (never sent to server)
 *   - remote_id     : nullable server-side id (null = not yet synced)
 *   - sincronizado  : 0 = offline only, 1 = synced with server
 *
 * The nested hierarchy is:
 *   estudos → campanhas → unidades_amostrais → eventos_amostragem → registros_ocorrencia
 *
 * Especies and Variaveis are scoped to a study (estudo).
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "estudos_offline.db"
        const val DB_VERSION = 4

        // ── TABLE NAMES ──────────────────────────────────────────────────────
        const val TABLE_ESTUDOS             = "estudos"
        const val TABLE_VARIAVEIS           = "variaveis"
        const val TABLE_ESPECIES            = "especies"
        const val TABLE_CAMPANHAS           = "campanhas"
        const val TABLE_VALORES_VARIAVEIS   = "valores_variaveis"
        const val TABLE_UNIDADES            = "unidades_amostrais"
        const val TABLE_EVENTOS             = "eventos_amostragem"
        const val TABLE_REGISTROS           = "registros_ocorrencia"
        const val TABLE_IMAGENS_CACHE       = "imagens_cache"

        // ── COMMON COLUMNS ───────────────────────────────────────────────────
        const val COL_LOCAL_ID      = "local_id"
        const val COL_REMOTE_ID     = "remote_id"
        const val COL_SINCRONIZADO  = "sincronizado"  // 0 = offline, 1 = synced
        const val COL_CREATED_AT    = "created_at"
        const val COL_UPDATED_AT    = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ESTUDOS)
        db.execSQL(SQL_CREATE_VARIAVEIS)
        db.execSQL(SQL_CREATE_ESPECIES)
        db.execSQL(SQL_CREATE_CAMPANHAS)
        db.execSQL(SQL_CREATE_VALORES_VARIAVEIS)
        db.execSQL(SQL_CREATE_UNIDADES)
        db.execSQL(SQL_CREATE_EVENTOS)
        db.execSQL(SQL_CREATE_REGISTROS)
        db.execSQL(SQL_CREATE_IMAGENS_CACHE)
        criarIndicesUnicidade(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(SQL_CREATE_IMAGENS_CACHE)
        }
        if (oldVersion < 3) {
            removerDuplicatasRemoteId(db)
            criarIndicesUnicidade(db)
        }
        if (oldVersion < 4) {
            migrarValoresVariaveisV4(db)
        }
    }

    private fun migrarValoresVariaveisV4(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE valores_variaveis_v4 (
                local_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                sincronizado      INTEGER NOT NULL DEFAULT 0,
                campanha_local_id INTEGER,
                unidade_local_id  INTEGER,
                evento_local_id   INTEGER,
                registro_local_id INTEGER,
                variavel_local_id INTEGER NOT NULL,
                variavel_remote_id INTEGER,
                valor             TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO valores_variaveis_v4
                (local_id, sincronizado, campanha_local_id, variavel_local_id, variavel_remote_id, valor)
            SELECT local_id, sincronizado, campanha_local_id, variavel_local_id, variavel_remote_id, valor
            FROM valores_variaveis
        """.trimIndent())
        db.execSQL("DROP TABLE valores_variaveis")
        db.execSQL("ALTER TABLE valores_variaveis_v4 RENAME TO valores_variaveis")
    }

    /**
     * Remove linhas duplicadas pelo mesmo (parentLocalId, remoteId) antes de
     * criar os índices UNIQUE. Mantém a linha mais antiga (menor local_id).
     */
    private fun removerDuplicatasRemoteId(db: SQLiteDatabase) {
        val alvos = listOf(
            TABLE_ESTUDOS    to null,
            TABLE_VARIAVEIS  to "estudo_local_id",
            TABLE_ESPECIES   to "estudo_local_id",
            TABLE_CAMPANHAS  to "estudo_local_id",
            TABLE_UNIDADES   to "campanha_local_id",
            TABLE_EVENTOS    to "unidade_local_id",
            TABLE_REGISTROS  to "evento_local_id"
        )
        alvos.forEach { (tabela, parentCol) ->
            val grupo = if (parentCol != null) "$parentCol, $COL_REMOTE_ID" else COL_REMOTE_ID
            db.execSQL(
                """
                DELETE FROM $tabela
                WHERE $COL_REMOTE_ID IS NOT NULL
                  AND $COL_LOCAL_ID NOT IN (
                    SELECT MIN($COL_LOCAL_ID) FROM $tabela
                    WHERE $COL_REMOTE_ID IS NOT NULL
                    GROUP BY $grupo
                  )
                """.trimIndent()
            )
        }
    }

    /**
     * Índices UNIQUE parciais. A cláusula WHERE remote_id IS NOT NULL permite
     * que múltiplas linhas offline (remote_id=NULL) coexistam, ao passo que
     * duplicatas com o mesmo remote_id dentro do mesmo pai são proibidas.
     */
    private fun criarIndicesUnicidade(db: SQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_ESTUDOS}_remote    ON $TABLE_ESTUDOS($COL_REMOTE_ID)                     WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_VARIAVEIS}_remote  ON $TABLE_VARIAVEIS(estudo_local_id, $COL_REMOTE_ID) WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_ESPECIES}_remote   ON $TABLE_ESPECIES(estudo_local_id, $COL_REMOTE_ID)  WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_CAMPANHAS}_remote  ON $TABLE_CAMPANHAS(estudo_local_id, $COL_REMOTE_ID) WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_UNIDADES}_remote   ON $TABLE_UNIDADES(campanha_local_id, $COL_REMOTE_ID) WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_EVENTOS}_remote    ON $TABLE_EVENTOS(unidade_local_id, $COL_REMOTE_ID)   WHERE $COL_REMOTE_ID IS NOT NULL")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${TABLE_REGISTROS}_remote  ON $TABLE_REGISTROS(evento_local_id, $COL_REMOTE_ID)  WHERE $COL_REMOTE_ID IS NOT NULL")
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    private val SQL_CREATE_ESTUDOS = """
        CREATE TABLE $TABLE_ESTUDOS (
            $COL_LOCAL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID    INTEGER,
            $COL_SINCRONIZADO INTEGER NOT NULL DEFAULT 0,
            nome              TEXT NOT NULL,
            observacoes       TEXT,
            perfil            TEXT,
            $COL_CREATED_AT   TEXT,
            $COL_UPDATED_AT   TEXT
        )
    """.trimIndent()

    private val SQL_CREATE_VARIAVEIS = """
        CREATE TABLE $TABLE_VARIAVEIS (
            $COL_LOCAL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID    INTEGER,
            $COL_SINCRONIZADO INTEGER NOT NULL DEFAULT 0,
            estudo_local_id   INTEGER NOT NULL,
            nome              TEXT NOT NULL,
            nivel_aplicacao   TEXT NOT NULL,
            tipo_dado         TEXT NOT NULL,
            metrica           TEXT,
            $COL_CREATED_AT   TEXT,
            $COL_UPDATED_AT   TEXT,
            FOREIGN KEY(estudo_local_id) REFERENCES $TABLE_ESTUDOS($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_ESPECIES = """
        CREATE TABLE $TABLE_ESPECIES (
            $COL_LOCAL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID       INTEGER,
            $COL_SINCRONIZADO    INTEGER NOT NULL DEFAULT 0,
            estudo_local_id      INTEGER NOT NULL,
            foto                 TEXT,
            classe               TEXT NOT NULL,
            ordem                TEXT NOT NULL,
            familia              TEXT NOT NULL,
            genero               TEXT NOT NULL,
            especie              TEXT NOT NULL,
            nome_popular         TEXT,
            status_conservacao   TEXT,
            endemismo            INTEGER NOT NULL DEFAULT 0,
            $COL_CREATED_AT      TEXT,
            FOREIGN KEY(estudo_local_id) REFERENCES $TABLE_ESTUDOS($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_CAMPANHAS = """
        CREATE TABLE $TABLE_CAMPANHAS (
            $COL_LOCAL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID    INTEGER,
            $COL_SINCRONIZADO INTEGER NOT NULL DEFAULT 0,
            estudo_local_id   INTEGER NOT NULL,
            nome              TEXT NOT NULL,
            data_inicio       TEXT NOT NULL,
            data_fim          TEXT,
            descricao         TEXT,
            $COL_CREATED_AT   TEXT,
            $COL_UPDATED_AT   TEXT,
            FOREIGN KEY(estudo_local_id) REFERENCES $TABLE_ESTUDOS($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_VALORES_VARIAVEIS = """
        CREATE TABLE $TABLE_VALORES_VARIAVEIS (
            $COL_LOCAL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_SINCRONIZADO   INTEGER NOT NULL DEFAULT 0,
            campanha_local_id   INTEGER,
            unidade_local_id    INTEGER,
            evento_local_id     INTEGER,
            registro_local_id   INTEGER,
            variavel_local_id   INTEGER NOT NULL,
            variavel_remote_id  INTEGER,
            valor               TEXT NOT NULL,
            FOREIGN KEY(campanha_local_id)  REFERENCES $TABLE_CAMPANHAS($COL_LOCAL_ID),
            FOREIGN KEY(unidade_local_id)   REFERENCES $TABLE_UNIDADES($COL_LOCAL_ID),
            FOREIGN KEY(evento_local_id)    REFERENCES $TABLE_EVENTOS($COL_LOCAL_ID),
            FOREIGN KEY(registro_local_id)  REFERENCES $TABLE_REGISTROS($COL_LOCAL_ID),
            FOREIGN KEY(variavel_local_id)  REFERENCES $TABLE_VARIAVEIS($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_UNIDADES = """
        CREATE TABLE $TABLE_UNIDADES (
            $COL_LOCAL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID    INTEGER,
            $COL_SINCRONIZADO INTEGER NOT NULL DEFAULT 0,
            campanha_local_id INTEGER NOT NULL,
            nome              TEXT NOT NULL,
            latitude          REAL NOT NULL,
            longitude         REAL NOT NULL,
            raio              REAL,
            metodo_coleta     TEXT,
            esforco_amostral  TEXT,
            $COL_CREATED_AT   TEXT,
            $COL_UPDATED_AT   TEXT,
            FOREIGN KEY(campanha_local_id) REFERENCES $TABLE_CAMPANHAS($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_EVENTOS = """
        CREATE TABLE $TABLE_EVENTOS (
            $COL_LOCAL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID      INTEGER,
            $COL_SINCRONIZADO   INTEGER NOT NULL DEFAULT 0,
            unidade_local_id    INTEGER NOT NULL,
            horario_inicio      TEXT NOT NULL,
            horario_fim         TEXT,
            esforco_real        TEXT,
            $COL_CREATED_AT     TEXT,
            FOREIGN KEY(unidade_local_id) REFERENCES $TABLE_UNIDADES($COL_LOCAL_ID)
        )
    """.trimIndent()

    private val SQL_CREATE_IMAGENS_CACHE = """
        CREATE TABLE $TABLE_IMAGENS_CACHE (
            url         TEXT PRIMARY KEY,
            bytes       BLOB NOT NULL,
            $COL_CREATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    private val SQL_CREATE_REGISTROS = """
        CREATE TABLE $TABLE_REGISTROS (
            $COL_LOCAL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REMOTE_ID      INTEGER,
            $COL_SINCRONIZADO   INTEGER NOT NULL DEFAULT 0,
            evento_local_id     INTEGER NOT NULL,
            especie_local_id    INTEGER NOT NULL,
            especie_remote_id   INTEGER,
            data                TEXT NOT NULL,
            hora                TEXT NOT NULL,
            latitude            REAL NOT NULL,
            longitude           REAL NOT NULL,
            qtde_individuos     INTEGER,
            foto                TEXT,
            ausencia_especie    INTEGER,
            $COL_CREATED_AT     TEXT,
            FOREIGN KEY(evento_local_id) REFERENCES $TABLE_EVENTOS($COL_LOCAL_ID),
            FOREIGN KEY(especie_local_id) REFERENCES $TABLE_ESPECIES($COL_LOCAL_ID)
        )
    """.trimIndent()
}
