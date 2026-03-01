package io.github.rothes.esu.core.storage

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

fun Table.userId(
    columnName: String = "user",
    fkName: String = "fk_${tableName.lowercase()}__${columnName}__id"
) = integer(columnName)
    .references(
        ref = StorageManager.UsersTable.dbId,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
        fkName = fkName
    )