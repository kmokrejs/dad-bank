import type { ReactNode } from 'react'
import { t } from '../../i18n'
import './Table.css'

export interface Column<T> {
  key: string
  header: ReactNode
  render: (row: T) => ReactNode
  align?: 'left' | 'right'
}

interface TableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  empty?: ReactNode
}

export function Table<T>({ columns, rows, rowKey, empty }: TableProps<T>) {
  const emptyText = empty ?? t('tx.empty')
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>{columns.map((c) => <th key={c.key} className={c.align === 'right' ? 'table__right' : ''}>{c.header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr><td colSpan={columns.length} className="table__empty">{emptyText}</td></tr>
          ) : rows.map((r) => (
            <tr key={rowKey(r)}>
              {columns.map((c) => <td key={c.key} className={c.align === 'right' ? 'table__right' : ''}>{c.render(r)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
