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

export class QwcCamelMicrometer extends QwcCamelCore {

    _labelMap = {
        counters: 'Counter',
        gauges: 'Gauge',
        timers: 'Timer',
        longTaskTimer: 'Long Task Timer',
        distributionSummary: 'Distribution Summary'
    };

    _metricTypeData = [{
        'label': '',
        'value': ''
    }];

    static properties = {
        data: {state: true},
        filterText: {state: true},
        selectedMetricType: {state: true},
        includeTags: {state: true}
    }

    constructor() {
        super('micrometer', {});
        this.data = [];
        this.filterText = '';
        Object.keys(this._labelMap).forEach(type => {
            this._metricTypeData.push({'label': this._labelMap[type], 'value': type});
        });
        this.includeTags = true;
    }

    filteredData() {
        if (!this.filterText && !this.selectedMetricType) {
            return this.data;
        }
        const text = this.filterText.toLowerCase();
        return this.data
            .filter(item => !this.selectedMetricType || item.type === this.selectedMetricType)
            .filter(item => !this.filterText || item.name.toLowerCase().includes(text));
    }

    render() {
        const consoleData = super.consoleData();

        // We need to bend the original Micrometer console response to something usable in vaadin-grid
        // TODO: Do this in the Camel Micrometer DevConsole
        this.data = [];
        Object.keys(this._labelMap).forEach(type => {
            if (consoleData[type]) {
                consoleData[type].forEach(metric => {
                    metric.type = type;
                    this.data.push(metric);
                });
            }
        });

        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 60%"
                        placeholder="Filter by metric name"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-combo-box @change="${e => this.selectedMetricType = e.target.value}"
                                  style="width: 20%"
                                  placeholder="Filter by metric type"
                                  item-label-path="label"
                                  item-value-path="value"
                                  .items="${this._metricTypeData}"
                                  clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-combo-box>
                <vaadin-checkbox label="Show tags"
                                 @change="${() => this._includeTags()}"
                                 .checked="${this.includeTags}"
                                 style="width: 20%">
                </vaadin-checkbox>
            </vaadin-horizontal-layout>

            <vaadin-grid .items="${this.filteredData()}" class="consoleData" theme="no-border row-stripes"">
                <vaadin-grid-sort-column
                        path="name"
                        header="Name"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(item.name), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="type"
                        header="Type"
                        ${columnBodyRenderer((item) => super.codeStyleRenderer(this._labelMap[item.type]), [])}
                        width="150px"
                        flex-grow="0"
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-column
                        header="Tags"
                        ${columnBodyRenderer((item) => this._renderTags(item.tags), [])}
                        resizable
                        ?hidden="${!this.includeTags}">
                </vaadin-grid-column>
                <vaadin-grid-column
                        header="Value"
                        ${columnBodyRenderer((item) => this._renderMetric(item), [])}
                        resizable>
                </vaadin-grid-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `;
    }

    _renderMetric(metric) {
        if (metric.type === 'counters') {
            return this._renderCounter(metric);
        } else if (metric.type === 'gauges') {
            return this._renderGauge(metric);
        } else if (metric.type === 'timers') {
            return this._renderTimer(metric);
        } else if (metric.type === 'longTaskTimer') {
            return this._renderLongTaskTimer(metric);
        } else if (metric.type === 'distributionSummary') {
            return this._renderDistributionSummary(metric);
        } else {
            return html`<code>Unknown</code>`;
        }
    }

    _renderCounter(counter) {
        return super.numericPillStyleRenderer(counter.count);
    }

    _renderGauge(gauge) {
        return super.numericPillStyleRenderer(gauge.value);
    }

    _renderTimer(timer) {
        return html`
            <vaadin-horizontal-layout theme="wrap" style="gap: 10px">
                <qui-badge small pill><span>Count=${timer.count}</span></qui-badge>
                <qui-badge small pill><span>Mean=${timer.mean}</span></qui-badge>
                <qui-badge small pill><span>Max=${timer.max}</span></qui-badge>
                <qui-badge small pill><span>Total=${timer.total}</span></qui-badge>
            </vaadin-horizontal-layout>
        `;
    }

    _renderLongTaskTimer(longTaskTimer) {
        return html`
            <vaadin-horizontal-layout theme="wrap" style="gap: 10px">
                <qui-badge small pill><span>Active Tasks=${longTaskTimer.activeTasks}</span></qui-badge>
                <qui-badge small pill><span>Mean=${longTaskTimer.mean}</span></qui-badge>
                <qui-badge small pill><span>Max=${longTaskTimer.max}</span></qui-badge>
                <qui-badge small pill><span>Duration=${longTaskTimer.duration}</span></qui-badge>
            </vaadin-horizontal-layout>
        `;
    }

    _renderDistributionSummary(distributionSummary) {
        return html`
            <vaadin-horizontal-layout theme="wrap" style="gap: 10px">
                <qui-badge small pill><span>Count=${distributionSummary.count}</span></qui-badge>
                <qui-badge small pill><span>Mean=${distributionSummary.mean}</span></qui-badge>
                <qui-badge small pill><span>Max=${distributionSummary.max}</span></qui-badge>
                <qui-badge small pill><span>Total=${distributionSummary.totalAmount}</span></qui-badge>
            </vaadin-horizontal-layout>
        `;
    }

    _renderTags(tags) {
        if (tags && tags.length > 0) {
            return html`
                <vaadin-horizontal-layout theme="wrap" style="gap: 10px">
                    ${tags.map(tag => {
                        return html`
                            <qui-badge small pill><span>${tag.key}=${tag.value}</span></qui-badge>`;
                    })}
                </vaadin-horizontal-layout>
            `;
        }
    }

    _includeTags() {
        this.includeTags = !this.includeTags;
    }
}

customElements.define('qwc-camel-micrometer', QwcCamelMicrometer);
