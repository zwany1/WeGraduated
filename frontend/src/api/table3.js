import api from './index'

export function generateTable3(data) {
  return api.post('/table3/generate', data, { responseType: 'blob' })
}

export function parseSql(sql) {
  return api.post('/table3/parse-sql', { sql })
}
