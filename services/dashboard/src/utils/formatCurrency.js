/**
 * Format currency amount based on currency type
 * TWD and JPY: no decimal places
 * Other currencies: 2 decimal places
 */
export const formatCurrency = (amount, currency = 'USD') => {
  const noDecimalCurrencies = ['TWD', 'JPY']
  const minimumFractionDigits = noDecimalCurrencies.includes(currency) ? 0 : 2
  const maximumFractionDigits = noDecimalCurrencies.includes(currency) ? 0 : 2

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits,
    maximumFractionDigits,
  }).format(amount)
}
