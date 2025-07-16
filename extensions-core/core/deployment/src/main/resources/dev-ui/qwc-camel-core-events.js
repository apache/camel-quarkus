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

export class QwcCamelCoreEvents extends QwcCamelCore {

    _eventTypes = {
        exchangeEvents: 'Exchange events',
        routeEvents: 'Route events',
        events: 'Other events',
    };

    _eventTypeData = [{
        'label': '',
        'value': ''
    }];

    static properties = {
        data: {state: true},
        filterText: {state: true},
        selectedEventType: {state: true},
    }

    constructor() {
        super('event', {});
        this.data = [];
        this.filterText = '';
        Object.keys(this._eventTypes).forEach(type => {
            this._eventTypeData.push({'label': this._eventTypes[type], 'value': type});
        });
    }

    filteredData() {
        if (!this.filterText && !this.selectedEventType) {
            return this.data;
        }
        const text = this.filterText.toLowerCase();

        return this.data
            .filter(item => !this.selectedEventType || item.internalType === this.selectedEventType)
            .filter(item => !this.filterText || item.name.toLowerCase().includes(text));
    }

    render() {
        this.data = [];
        Object.keys(super.consoleData()).forEach(eventType => {
            if (super.consoleData()[eventType]) {
                super.consoleData()[eventType].forEach(event => {
                    event.internalType = eventType;
                    this.data.push(event);
                });
            }
        });

        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-combo-box @change="${e => this.selectedEventType = e.target.value}"
                                  style="width: 20%"
                                  placeholder="Filter by event type"
                                  item-label-path="label"
                                  item-value-path="value"
                                  .items="${this._eventTypeData}"
                                  clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-combo-box>
            </vaadin-horizontal-layout>

            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                        path="type"
                        auto-width
                        header="Type"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.type), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="timestamp"
                        auto-width
                        header="Timestamp"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(super.toDate(item.timestamp)), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-column
                        path="message"
                        header="Message"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.message), [])}
                        resizable>
                </vaadin-grid-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `;
    }
}

customElements.define('qwc-camel-core-events', QwcCamelCoreEvents);
