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

export class QwcCamelCoreContext extends QwcCamelCore {

    constructor() {
        super('context', {});
    }

    render() {
        if (Object.entries(super.consoleData()).length === 0) {
            return super.redenderNoDataAvailableMessage();
        }

        return html`
            <vaadin-grid .items="${[super.consoleData()]}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-column
                        auto-width
                        header="Name"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.name), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Version"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.version), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="State"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.state), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Uptime"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(super.formatUptime(item.uptime)), [])}
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

                <vaadin-grid-column
                        auto-width
                        header="Exchanges Total"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesTotal), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Exchanges Failed"
                        ${columnBodyRenderer((item) => super.numericPillStyleRenderer(item.statistics.exchangesFailed), [])}
                        resizable>
                </vaadin-grid-column>

                <vaadin-grid-column
                        auto-width
                        header="Exchanges Inflight"
                        ${columnBodyRenderer((item) => super.   numericPillStyleRenderer(item.statistics.exchangesInflight), [])}
                        resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

}

customElements.define('qwc-camel-core-context', QwcCamelCoreContext);
