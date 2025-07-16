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
import {html} from 'qwc-hot-reload-element';
import {QwcCamelCore} from "./qwc-camel-core.js";
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';

export class QwcCamelCoreVariables extends QwcCamelCore {

    static properties = {
        data: { state: true },
        filterText: { state: true },
    }

    constructor() {
        super('variables', {});
        this.data = [];
        this.filterText = '';
    }

    filteredData() {
        if (!this.filterText)  {
            return this.data;
        }
        const text = this.filterText.toLowerCase();
        return this.data.filter(item =>
            item.key.toLowerCase().includes(text)
        );
    }

    render() {
        // Flatten the results to make them easier to work with in vaadin-grid
        this.data = Object.entries(super.consoleData()).flatMap(([source, items]) =>
            items.map(item => ({...item, source}))
        );

        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 100%"
                        placeholder="Filter variable name"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
            </vaadin-horizontal-layout>

            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        auto-width
                        path="source"
                        header="Source"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.source, 'capitialized'), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        auto-width
                        path="key"
                        header="Name"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.key), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="value"
                        header="Value"
                        ${columnBodyRenderer((item) => this._renderVariableValue(item.value), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="type"
                        header="Type"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.type), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `;
    }

    _renderVariableValue(value) {
        if (value) {
            if (typeof value === 'string' && value.startsWith("[")) {
                // Handle the variable value being a raw toString dump of a collection
                try {
                    const fixed = value.replace(/([^\[\],\s]+)/g, '"$1"');
                    const json = JSON.parse(fixed);
                    if (Array.isArray(json)) {
                        return html `
                            <code>Collection</code> <qui-badge small pill><span>${json.length}</span></qui-badge>
                        `;
                    }
                } catch (e) {
                    // Ignored
                }
            } else if (Array.isArray(value)) {
                return html `
                    <code>Collection</code> <qui-badge small pill><span>${value.length}</span></qui-badge>
                `;
            } else if (typeof value === 'object') {
                // If it's an object, just count the fields
                return html `
                    <code>Object</code> <qui-badge small pill><span>${Object.keys(value).length}</span></qui-badge>
                `;
            }
        }
        return super.codeStyleRenderer(value);
    }
}

customElements.define('qwc-camel-core-variables', QwcCamelCoreVariables);
