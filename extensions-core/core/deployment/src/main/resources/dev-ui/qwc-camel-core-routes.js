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

export class QwcCamelCoreRoutes extends QwcCamelCore {

    static properties = {
        data: { state: true },
        filterText: { state: true },
    }

    constructor() {
        super('route', {});
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
        this.data = this.consoleData()['routes'];
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 80%"
                        placeholder="Filter endpoint URI"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-integer-field
                        style="width: 20%"
                        step-buttons-visible
                        placeholder="Route limit"
                        min="0"
                        max="2147483647"
                        @value-changed="${(e) => {
                            super.putOption('limit', e.detail.value);
                        }}">
                </vaadin-integer-field>
            </vaadin-horizontal-layout>
            ${super.consoleData()['routes'] === undefined ? super.redenderNoDataAvailableMessage() : this._renderRouteData()}
        `;
    }

    _renderRouteData() {
        return html`
            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="routeId"
                        auto-width
                        header="Route ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.routeId), [])}
                        resizable>
                </vaadin-grid-sort-column>

                <vaadin-grid-column
                        header="From"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.from), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Source"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.source), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-sort-column
                        path="state"
                        auto-width
                        header="State"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.state), [])}
                        resizable>
                </vaadin-grid-sort-column>

                <vaadin-grid-column
                        auto-width
                        header="Uptime"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.uptime), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Idle Since"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.idleSince), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Exchanges Throughput"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesThroughput), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-sort-column
                        path="statistics.exchangesTotal"
                        auto-width
                        header="Exchanges Total"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesTotal), [])}
                        resizable>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column
                        path="statistics.exchangesFailed"
                        auto-width
                        header="Exchanges Failed"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesFailed), [])}
                        resizable>
                </vaadin-grid-sort-column>

                <vaadin-grid-sort-column
                        path="statistics.exchangesInflight"
                        auto-width
                        header="Exchanges Inflight"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesInflight), [])}
                        resizable>
                </vaadin-grid-sort-column>
            </vaadin-grid>
        `;
    }

}

customElements.define('qwc-camel-core-routes', QwcCamelCoreRoutes);
