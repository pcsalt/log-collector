package com.pcsalt.logcollector.config

import org.springframework.data.relational.core.dialect.AbstractDialect
import org.springframework.data.relational.core.dialect.ArrayColumns
import org.springframework.data.relational.core.dialect.IdGeneration
import org.springframework.data.relational.core.dialect.LimitClause
import org.springframework.data.relational.core.dialect.LockClause
import org.springframework.data.relational.core.sql.LockOptions

class SqliteDialect : AbstractDialect() {

  companion object {
    val INSTANCE = SqliteDialect()
  }

  override fun limit(): LimitClause {
    return object : LimitClause {
      override fun getLimit(limit: Long): String = "LIMIT $limit"

      override fun getOffset(offset: Long): String = "OFFSET $offset"

      override fun getLimitOffset(limit: Long, offset: Long): String {
        return "LIMIT $limit OFFSET $offset"
      }

      override fun getClausePosition(): LimitClause.Position = LimitClause.Position.AFTER_ORDER_BY
    }
  }

  override fun lock(): LockClause {
    return object : LockClause {
      override fun getLock(lockOptions: LockOptions): String = ""
      override fun getClausePosition(): LockClause.Position = LockClause.Position.AFTER_ORDER_BY
    }
  }

  override fun getArraySupport(): ArrayColumns = ArrayColumns.Unsupported.INSTANCE

  override fun getIdGeneration(): IdGeneration {
    return object : IdGeneration {
      override fun driverRequiresKeyColumnNames(): Boolean = false
    }
  }
}
