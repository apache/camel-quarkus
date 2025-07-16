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

export class QwcCamelCoreBrowse extends QwcCamelCore {

    static properties = {
        data: { state: true },
        filterText: { state: true },
    }

    constructor() {
        super('browse', {'dump': 'false'});
        this.data = [];
        this.filterText = '';
    }

    filteredData() {
        if (!this.filterText)  {
            return this.data;
        }
        const text = this.filterText.toLowerCase();
        return this.data.filter(item =>
            item.endpointUri.toLowerCase().includes(text)
        );
    }

    render() {
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 100%"
                        placeholder="Filter endpoint URI"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
            </vaadin-horizontal-layout>
            ${super.consoleData()['browse'] === undefined ? super.redenderNoDataAvailableMessage() : this._renderBrowseData()}
        `;
    }

    _renderBrowseData() {
        this.data = this.consoleData()['browse'];
        return html`
            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="endpointUri"
                        header="Endpoint URI"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.endpointUri), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="queueSize"
                        auto-width
                        header="Queue Size"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.queueSize), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="firstTimestamp"
                        auto-width
                        header="First Timestamp"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(super.toDate(item.firstTimestamp)), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="lastTimestamp"
                        auto-width
                        header="Last Timestamp"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(super.toDate(item.lastTimestamp)), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `
    }
}

customElements.define('qwc-camel-core-browse', QwcCamelCoreBrowse);
