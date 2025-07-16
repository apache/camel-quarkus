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

export class QwcCamelCoreInflightExchanges extends QwcCamelCore {

    constructor() {
        super('inflight', {});
    }

    render() {
        if (super.consoleData()['inflight'] === 0) {
            return super.redenderNoDataAvailableMessage();
        }

        return html`
            <vaadin-grid .items="${super.consoleData()['exchanges']}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="exchangeId"
                        auto-width
                        header="Exchange ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.exchangeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="fromRouteId"
                        auto-width
                        header="From Route ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.fromRouteId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="atRouteId"
                        auto-width
                        header="At Route ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.atRouteId), [])}
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
                        header="Age"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(super.formatUptime(item.duration)), [])}
                        resizable>
                </vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}

customElements.define('qwc-camel-core-inflight-exchanges', QwcCamelCoreInflightExchanges);
