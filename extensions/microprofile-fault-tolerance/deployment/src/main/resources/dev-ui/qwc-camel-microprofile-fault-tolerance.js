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
import {QwcCamelCore} from "../camel-quarkus-core/qwc-camel-core.js";
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';

export class QwcCamelMicroprofileFaultTolerance extends QwcCamelCore {

    constructor() {
        super('fault-tolerance', {});
    }

    render() {
        return html`
            <vaadin-grid .items="${super.consoleData()['circuitBreakers']}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        auto-width
                        path="id"
                        header="ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.id), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        auto-width
                        path="routeId"
                        header="Route ID"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.routeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="state"
                        header="State"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.state), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `;
    }
}

customElements.define('qwc-camel-microprofile-fault-tolerance', QwcCamelMicroprofileFaultTolerance);
