/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = {
  alias: (name, aliases) => {
    for (expr of (aliases || '').split(',')) {
      const {pattern, alias} = splitOnce(expr)
      const re = new RegExp(pattern, 'i')
      if (re.test(name)) {
        return alias
      }
    }
    return ''
  },

  boldLink: (text, idPrefix, suffix = '') => {
    const idText = `_${idPrefix}_${text.split('.').join('_')}`
    text = suffix ? `*${text}* (${suffix})` : `*${text}*`
    return  `[#${idText}]\nxref:#${idText}['',role=anchor]${text}`
  },

  description: (value) => {
    try {
      return module.exports.strong(value, 'Autowired')
        + module.exports.strong(value, 'Required')
        + module.exports.strong(value, 'Deprecated')
        + (value.description ? module.exports.escapeAttributes(value.description) + (value.description.endsWith('.') ? '' : '.') : '')
        + (value.deprecatedNote ? `\n\nNOTE: ${value.deprecatedNote}` : '')
        + (value.enum ? `${['\n\nEnum values:\n'].concat(value.enum).join('\n* ')}` : '')
    } catch (e) {
      console.log('error', e)
      return e.msg()
    }
  },

  escapeAttributes: (text) => {
    return text ? text.split('{').join('\\{') : text
  },

  formatSignature: (signature) => {
    return signature.split('$').join('.') + ';'
  },

  javaSimpleName: (name) => {
    return name.split(/<.*>/).join('').split('.').pop()
  },

  pascalCase: (input) => {
    return input ?
      input.split('-').map((segment) => {
        return segment.length ?
          segment.charAt(0).toUpperCase() + segment.slice(1) :
          segment
      }).join('') :
      input
  },

  producerConsumerLong: (consumerOnly, producerOnly) => {
    if (consumerOnly) return 'Only consumer is supported'
    if (producerOnly) return 'Only producer is supported'
    return 'Both producer and consumer are supported'
  },

  strong: (data, text) => {
    return data[text.toLowerCase()] ? `*${text}* ` : ''
  },

  valueAsString: (value) => {
    return value === undefined ? '' : `${value}`
  },
}

function splitOnce (querySpec, token = '=') {
  const index = querySpec.indexOf(token)
  const pattern = querySpec.substring(0, index).trim()
  const alias = querySpec.substring(index + 1).trim()
  return { pattern, alias }
}
