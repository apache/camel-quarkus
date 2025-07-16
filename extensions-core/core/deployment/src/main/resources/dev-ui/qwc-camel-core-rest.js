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
import { html } from 'qwc-hot-reload-element';
import {QwcCamelCore} from "./qwc-camel-core.js";
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';

export class QwcCamelCoreRest extends QwcCamelCore {

    _httpMethods = {
        GET: 'GET',
        POST: 'POST',
        PUT: 'PUT',
        DELETE: 'DELETE',
        PATCH: 'PATCH',
        HEAD: 'HEAD',
        OPTIONS: 'OPTIONS',
        CONNECT: 'CONNECT',
        TRACE: 'TRACE'
    };

    _httpMethodData = [{
        'label': '',
        'value': ''
    }];

    static properties = {
        data: {state: true},
        filterText: {state: true},
        selectedHttpMethod: {state: true},
    }

    constructor() {
        super('rest', {});
        this.data = [];
        this.filterText = '';
        Object.keys(this._httpMethods).forEach(type => {
            this._httpMethodData.push({'label': this._httpMethods[type], 'value': type});
        });
    }

    filteredData() {
        if (!this.filterText && !this.selectedHttpMethod) {
            return this.data;
        }
        const text = this.filterText.toLowerCase();
        return this.data
            .filter(item => !this.selectedHttpMethod || item.method.toUpperCase() === this.selectedHttpMethod)
            .filter(item => !this.filterText || item.url.toLowerCase().includes(text));
    }

    render() {
        this.data = super.consoleData()['rests'];
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 80%"
                        placeholder="Filter by URL"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-combo-box @change="${e => this.selectedHttpMethod = e.target.value}"
                                  style="width: 20%"
                                  placeholder="Filter by HTTP method"
                                  item-label-path="label"
                                  item-value-path="value"
                                  .items="${this._httpMethodData}"
                                  clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-combo-box>
            </vaadin-horizontal-layout>

            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="url"
                        header="URL"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.url), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="method"
                        header="Method"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.method, 'httpMethod'), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="contractFirst"
                        header="Contract First"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.contractFirst), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="state"
                        header="State"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.state), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="consumes"
                        header="Consumes"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.consumes), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="produces"
                        header="Produces"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.produces), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="inType"
                        header="In Type"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.inType), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="outType"
                        header="Out Type"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.outType), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="description"
                        header="Description"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.description), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>`;
    }
}

customElements.define('qwc-camel-core-rest', QwcCamelCoreRest);
