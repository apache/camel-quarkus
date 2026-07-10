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
import { css, html, LitElement } from 'lit';
import '@vaadin/checkbox';
import '@vaadin/horizontal-layout';
import '@vaadin/tabs';

export class QwcCamelCoreDiagram extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
        }

        .controls {
            padding: 8px 16px;
            flex-shrink: 0;
        }

        .diagram-container {
            flex: 1;
            padding: 0 16px 16px;
            min-height: 0;
            overflow: auto;
        }

        camel-route-diagram,
        camel-topology-diagram {
            --crd-bg: #ffffff;
            --crd-fg: #1e293b;
            --crd-edge: #94a3b8;
            --ctd-bg: #ffffff;
            --ctd-fg: #1e293b;
            --ctd-edge: #94a3b8;
        }
    `;

    static properties = {
        _mode: { state: true },
        _autoRefresh: { state: true },
        _loaded: { state: true },
        _error: { state: true },
    };

    constructor() {
        super();
        this._mode = 'route';
        this._autoRefresh = true;
        this._loaded = false;
        this._error = null;
        this._loadWebComponents();
    }

    _getNonAppRoot() {
        const pathname = window.location.pathname;
        const idx = pathname.indexOf('dev-ui/');
        return idx >= 0 ? pathname.substring(0, idx) : '/q/';
    }

    _loadWebComponents() {
        Promise.all([
            import('/camel/diagram/camel-route-diagram.js'),
            import('/camel/diagram/camel-topology-diagram.js'),
        ]).then(() => {
            this._loaded = true;
        }).catch(err => {
            console.error('Failed to load Camel diagram web components', err);
            this._error = 'Failed to load diagram components.';
        });
    }

    _getRefreshInterval() {
        return this._autoRefresh ? '5000' : '0';
    }

    render() {
        if (this._error) {
            return html`<p>${this._error}</p>`;
        }

        if (!this._loaded) {
            return html`<p>Loading diagram...</p>`;
        }

        const basePath = this._getNonAppRoot() + 'camel/diagram/';

        return html`
            <vaadin-horizontal-layout class="controls" theme="spacing" style="align-items: center;">
                <vaadin-tabs @selected-changed=${e => {
                    this._mode = e.detail.value === 0 ? 'route' : 'topology';
                }}>
                    <vaadin-tab>Routes</vaadin-tab>
                    <vaadin-tab>Topology</vaadin-tab>
                </vaadin-tabs>
                <vaadin-checkbox
                        label="Auto refresh"
                        ?checked="${this._autoRefresh}"
                        @change=${e => { this._autoRefresh = e.target.checked; }}>
                </vaadin-checkbox>
            </vaadin-horizontal-layout>
            <div class="diagram-container">
                ${this._mode === 'route'
                    ? html`<camel-route-diagram
                                src="${basePath}route-structure"
                                refresh="${this._getRefreshInterval()}">
                           </camel-route-diagram>`
                    : html`<camel-topology-diagram
                                src="${basePath}route-topology"
                                refresh="${this._getRefreshInterval()}">
                           </camel-topology-diagram>`
                }
            </div>
        `;
    }
}

customElements.define('qwc-camel-core-diagram', QwcCamelCoreDiagram);
