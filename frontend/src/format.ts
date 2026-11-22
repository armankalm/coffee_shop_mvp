export function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}

export function formatTime(value: string | Date) {
  const date = typeof value === 'string' ? new Date(value) : value
  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}

/** «1 позиция», «2 позиции», «5 позиций». */
export function pluralPositions(count: number) {
  const mod10 = count % 10
  const mod100 = count % 100
  if (mod10 === 1 && mod100 !== 11) return `${count} позиция`
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return `${count} позиции`
  return `${count} позиций`
}
