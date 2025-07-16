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

export class QwcCamelCoreBlockedExchanges extends QwcCamelCore {

    static properties = {
        data: { state: true },
        filterText: { state: true },
    }

    constructor() {
        super('blocked', {});
        this.data = [];
        this.filterText = '';
    }

    filteredData() {
        if (!this.filterText)  {
            return this.data;
        }
        const text = this.filterText.toLowerCase();
        return this.data.filter(item =>
            item.routeId.toLowerCase().includes(text)
        );
    }

    render() {
        if (super.consoleData()['blocked'] === 0) {
            return super.redenderNoDataAvailableMessage();
        }

        this.data = this.consoleData()['blocked'];
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 100%"
                        placeholder="Filter route ID"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
            </vaadin-horizontal-layout>
            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="routeId"
                        auto-width
                        header="Route ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.routeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="exchangeId"
                        auto-width
                        header="Exchange ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.exchangeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="nodeId"
                        auto-width
                        header="Node ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.nodeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="duration"
                        auto-width
                        header="Duration (ms)"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.duration), [])}
                        resizable>
                </vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}

customElements.define('qwc-camel-core-blocked-exchanges', QwcCamelCoreBlockedExchanges);
