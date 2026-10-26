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
import {css, html} from 'qwc-hot-reload-element';
import {QwcCamelCore} from "./qwc-camel-core.js";
import {columnBodyRenderer, gridRowDetailsRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import '@qomponent/qui-badge';
import 'qui-ide-link';

export class QwcCamelCoreErrors extends QwcCamelCore {

    static styles = [QwcCamelCore.styles, css`
        .details {
            display: flex;
            flex-direction: column;
            gap: 10px;
            padding: 5px 10px 15px 10px;
        }

        .details h4 {
            margin: 0;
        }

        .details pre {
            margin: 0;
            font-size: 85%;
            white-space: pre-wrap;
            word-break: break-all;
        }

        .details table {
            border-collapse: collapse;
            font-size: 85%;
        }

        .details td {
            padding: 2px 15px 2px 0;
            vertical-align: top;
        }
    `];

    _handledFilterData = [
        {'label': 'Unhandled', 'value': 'false'},
        {'label': 'Handled', 'value': 'true'},
    ];

    static properties = {
        filterText: {state: true},
        handledFilter: {state: true},
        _openedKinds: {state: true},
    }

    constructor() {
        super('errors', {'stackTrace': 'true'});
        this.filterText = '';
        this.handledFilter = '';
        this._openedKinds = new Set();
    }

    filteredData() {
        const errors = this.consoleData()['errors'] ?? [];
        const text = this.filterText.toLowerCase();
        return this._groupByKind(errors
            .filter(item => !this.handledFilter || String(item.handled) === this.handledFilter)
            .filter(item => !text
                || (item.routeId ?? '').toLowerCase().includes(text)
                || (item.nodeId ?? '').toLowerCase().includes(text)
                || (item.exception?.type ?? '').toLowerCase().includes(text)));
    }

    // Camel keeps only the newest few exchanges of the same kind of error (route, node and exception type)
    // and counts the rest, so show one row per kind
    _groupByKind(errors) {
        const groups = new Map();
        [...errors].sort((a, b) => b.timestamp - a.timestamp).forEach(entry => {
            const kind = `${entry.routeId}|${entry.nodeId}|${entry.exception?.type}`;
            let group = groups.get(kind);
            if (!group) {
                group = {
                    kind: kind,
                    latest: entry,
                    entries: [],
                    routeId: entry.routeId,
                    nodeId: entry.nodeId,
                    exceptionType: entry.exception?.type,
                    handled: entry.handled,
                    occurrences: 0,
                    firstTimestamp: entry.timestamp,
                    lastTimestamp: entry.timestamp,
                };
                groups.set(kind, group);
            }
            group.entries.push(entry);
            group.occurrences = Math.max(group.occurrences, entry.repeatCount ?? 1, group.entries.length);
            group.firstTimestamp = Math.min(group.firstTimestamp, entry.repeatFirstTimestamp ?? entry.timestamp);
        });
        return [...groups.values()];
    }

    render() {
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-text-field
                        style="width: 80%"
                        placeholder="Filter by route, node or exception type"
                        @input=${e => this.filterText = e.target.value}
                        clear-button-visible>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-combo-box
                        style="width: 20%"
                        placeholder="Filter by handled status"
                        item-label-path="label"
                        item-value-path="value"
                        .items="${this._handledFilterData}"
                        @change="${e => this.handledFilter = e.target.value}"
                        clear-button-visible>
                </vaadin-combo-box>
            </vaadin-horizontal-layout>
            ${this._renderContent()}
        `;
    }

    _renderContent() {
        const data = this.consoleData();
        if (data['enabled'] === undefined) {
            return super.redenderNoDataAvailableMessage();
        }
        if (!data['enabled']) {
            return html`
                <vaadin-horizontal-layout class="empty-container">
                    The Camel error registry is disabled. Set camel.errorRegistry.enabled=true to capture routing errors.
                </vaadin-horizontal-layout>`;
        }
        return this._renderErrors();
    }

    _renderErrors() {
        const groups = this.filteredData();
        return html`
            <vaadin-grid .items="${groups}"
                         .detailsOpenedItems="${groups.filter(group => this._openedKinds.has(group.kind))}"
                         item-id-path="kind"
                         class="consoleData"
                         theme="no-border row-stripes"
                         @click="${e => this._toggleDetails(e)}"
                         ${gridRowDetailsRenderer((group) => this._renderDetails(group), [])}>
                <vaadin-grid-sort-column
                        path="lastTimestamp"
                        auto-width
                        header="Last Seen"
                        ${columnBodyRenderer((group) => super.codeStyleRenderer(super.toDate(group.lastTimestamp)), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="routeId"
                        auto-width
                        header="Route ID"
                        ${columnBodyRenderer((group) => super.codeStyleRenderer(group.routeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="nodeId"
                        auto-width
                        header="Node ID"
                        ${columnBodyRenderer((group) => super.codeStyleRenderer(group.nodeId), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="exceptionType"
                        auto-width
                        header="Exception"
                        ${columnBodyRenderer((group) => html`<code title="${group.exceptionType ?? ''}">${this._simpleName(group.exceptionType)}</code>`, [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-column
                        header="Last Message"
                        ${columnBodyRenderer((group) => super.codeStyleRenderer(group.latest.exception?.message), [])}
                        resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                        auto-width
                        header="Source"
                        ${columnBodyRenderer((group) => super.codeStyleRenderer(group.latest.location), [])}
                        resizable>
                </vaadin-grid-column>
                <vaadin-grid-sort-column
                        path="occurrences"
                        auto-width
                        header="Occurrences"
                        ${columnBodyRenderer((group) => super.numericPillStyleRenderer(group.occurrences), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                        path="handled"
                        auto-width
                        header="Handled"
                        ${columnBodyRenderer((group) => this._handledRenderer(group.handled), [])}
                        resizable>
                </vaadin-grid-sort-column>
                <span slot="empty-state">No errors captured</span>
            </vaadin-grid>
        `;
    }

    _toggleDetails(e) {
        const context = e.currentTarget.getEventContext(e);
        // clicks inside the expanded details (e.g. selecting text or an IDE link) keep it open
        if (!context.item || context.section === 'details') {
            return;
        }
        const opened = new Set(this._openedKinds);
        if (!opened.delete(context.item.kind)) {
            opened.add(context.item.kind);
        }
        this._openedKinds = opened;
    }

    _renderDetails(group) {
        const item = group.latest;
        const message = item.message ?? {};
        return html`
            <div class="details">
                <table>
                    ${this._detailRow('First seen', super.toDate(group.firstTimestamp))}
                    ${this._detailRow('Last seen', super.toDate(group.lastTimestamp))}
                    ${this._detailRow('Occurrences', group.occurrences)}
                </table>
                <h4>Latest captured exchanges (${group.entries.length} of ${group.occurrences})</h4>
                <table>
                    ${group.entries.map(entry => html`
                        <tr>
                            <td><code>${super.toDate(entry.timestamp)}</code></td>
                            <td><code>${entry.exchangeId}</code></td>
                            <td>${this._handledRenderer(entry.handled)}</td>
                            <td><code>${entry.exception?.message ?? ''}</code></td>
                        </tr>`)}
                </table>
                <h4>Latest exchange</h4>
                <table>
                    ${this._detailRow('Exchange ID', item.exchangeId)}
                    ${this._detailRow('From', item.fromEndpointUri)}
                    ${this._detailRow('Endpoint', item.endpointUri)}
                    ${this._detailRow('Step ID', item.stepId)}
                    ${this._detailRow('Elapsed', item.elapsed !== undefined ? item.elapsed + ' ms' : undefined)}
                    ${this._detailRow('Thread', item.threadName)}
                </table>
                ${this._renderList('Message history', item.messageHistory)}
                ${this._renderStackTrace(item.exception)}
                ${this._renderKeyValues('Headers', message.headers)}
                ${message.body ? html`
                    <h4>Body <code>${message.body.type ?? ''}</code></h4>
                    ${message.body.value !== undefined ? html`<pre>${this._unescape(message.body.value)}</pre>` : ''}` : ''}
                ${this._renderKeyValues('Exchange properties', item.exchangeProperties)}
                ${this._renderKeyValues('Exchange variables', item.exchangeVariables)}
            </div>
        `;
    }

    _detailRow(label, value) {
        if (value === undefined || value === null || value === '') {
            return '';
        }
        return html`<tr><td>${label}</td><td><code>${value}</code></td></tr>`;
    }

    _renderList(title, lines) {
        if (!lines || lines.length === 0) {
            return '';
        }
        return html`
            <h4>${title}</h4>
            <pre>${lines.join('\n')}</pre>`;
    }

    _renderStackTrace(exception) {
        if (!exception?.stackTrace) {
            return '';
        }
        const lines = this._unescape(exception.stackTrace).split(/\r?\n/).filter(line => line.trim() !== '');
        return html`
            <h4>Stack trace</h4>
            <pre>${lines.map(line => html`<qui-ide-link stackTraceLine="${line.trim()}">${line}</qui-ide-link>\n`)}</pre>`;
    }

    _renderKeyValues(title, entries) {
        if (!entries || entries.length === 0) {
            return '';
        }
        return html`
            <h4>${title}</h4>
            <table>
                ${entries.map(entry => html`
                    <tr>
                        <td><code>${entry.key}</code></td>
                        <td><code>${entry.type ?? ''}</code></td>
                        <td><code>${entry.value ?? ''}</code></td>
                    </tr>`)}
            </table>`;
    }

    _handledRenderer(handled) {
        if (handled) {
            return html`<qui-badge small level="success"><span>Handled</span></qui-badge>`;
        }
        return html`<qui-badge small level="error"><span>Unhandled</span></qui-badge>`;
    }

    _simpleName(type) {
        if (!type) {
            return '';
        }
        return type.substring(type.lastIndexOf('.') + 1);
    }

    // Camel escapes stack traces and message bodies for JSON before the console JSON is serialized
    _unescape(value) {
        if (typeof value !== 'string') {
            return value ?? '';
        }
        return value.replace(/\\(u[0-9a-fA-F]{4}|.)/g, (match, escaped) => {
            if (escaped.length === 5) {
                return String.fromCharCode(parseInt(escaped.substring(1), 16));
            }
            return this._escapes[escaped] ?? escaped;
        });
    }

    _escapes = {'n': '\n', 'r': '\r', 't': '\t', 'b': '\b', 'f': '\f'};
}

customElements.define('qwc-camel-core-errors', QwcCamelCoreErrors);
