[libui.ktx](../README.md) / [Table](README.md)

# Table

`class Table<T> : `[`Disposable`](../-disposable/README.md)`<`[`uiTableModel`](../../libui/ui-table-model.md)`>`

Wrapper class for [uiTableModel](../../libui/ui-table-model.md)

### Types

| Name | Summary |
|---|---|
| [TableColumn](-table-column/README.md) | `inner class TableColumn<T>` |

### Constructors

| Name | Summary |
|---|---|
| [Table](-table.md) | `Table(data: List<`[`T`](-table-column/README.md#T)`>, handler: CPointer<`[`ktTableHandler`](../../libui/kt-table-handler/README.md)`> = nativeHeap.alloc<ktTableHandler>().ptr)`<br>Wrapper class for [uiTableModel](../../libui/ui-table-model.md) |

### Properties

| Name | Summary |
|---|---|
| [data](data.md) | `val data: List<`[`T`](-table-column/README.md#T)`>` |

### Inherited Properties

| Name | Summary |
|---|---|
| [disposed](../-disposable/disposed.md) | `val disposed: Boolean`<br>Returns `true` if object was disposed - in this case [dispose](../-disposable/dispose.md) will do nothing, all other operations are invalid and will `throw Error("Resource is disposed")`. |

### Functions

| Name | Summary |
|---|---|
| [background](background.md) | `fun background(get: (row: Int) -> `[`Color`](../../libui.ktx.draw/-color/README.md)`?): Unit` |
| [column](column.md) | `fun column(name: String, init: `[`TableColumn`](-table-column/README.md)`<`[`T`](-table-column/README.md#T)`>.() -> Unit): Unit` |
| [rowChanged](row-changed.md) | `fun rowChanged(index: Int): Unit` |
| [rowDeleted](row-deleted.md) | `fun rowDeleted(oldIndex: Int): Unit` |
| [rowInserted](row-inserted.md) | `fun rowInserted(newIndex: Int): Unit` |

### Inherited Functions

| Name | Summary |
|---|---|
| [dispose](../-disposable/dispose.md) | `open fun dispose(): Unit`<br>Dispose and free all allocated native resources. |

### Extension Functions

| Name | Summary |
|---|---|
| [image](../../libui.ktx.draw/image.md) | `fun `[`Table`](README.md)`<*>.image(width: Int, height: Int, block: `[`Image`](../../libui.ktx.draw/-image/README.md)`.() -> Unit = {}): `[`Image`](../../libui.ktx.draw/-image/README.md) |
