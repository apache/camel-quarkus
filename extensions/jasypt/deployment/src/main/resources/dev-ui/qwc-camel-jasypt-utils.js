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
import {LitElement, css, html} from 'lit';
import {JsonRpc} from 'jsonrpc';
import { notifier } from 'notifier';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/select';

export class QwcCamelJasyptUtils extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .container {
            margin-left: 10px;
        }
        .result-heading {
            text-transform: capitalize;
        }
    `;

    static properties = {
        _text: {type: String},
        _result: {type: String},
        _actions: {type: Array}
    };

    constructor() {
        super();

        this._text = "";
        this._result = "";
        this._action = "Encrypt";
        this._actions = [
            {
                label: "Encrypt",
                value: "encrypt"
            },
            {
                label: "Decrypt",
                value: "decrypt"
            },
        ];
    }

    render() {
        let results;
        if (this._result.length > 0) {
            results = html`
                <div>
                    <p class="result-heading">${this._action}ed result:</p>
                    <p><code id="result">${this._result}</code></p>
                    <vaadin-button 
                            @click="${(e) => this._copyToClipboard(e, 'Copy')}"
                            theme="small">
                        <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                        Copy
                    </vaadin-button>
                </div>
            `;
        }

        return html`
            <div class="container">
                <p>A Camel Jasypt utility to encrypt or decrypt a configuration property value.</p>
                <p>Jasypt is configured from <code>quarkus.camel.jasypt</code> properties in <code>application.properties</code>. Refer to the Camel Quarkus Jasypt extension documentation for details.</p>
                <div>
                    <vaadin-select
                            label="Jasypt Action"
                            .items="${this._actions}"
                            .value="${this._actions[0].value}"
                            @value-changed="${(e) => this._selectAction(e.target.value)}" />
                </div>
                <div>
                    <vaadin-text-field
                            style="width: 50%;"
                            label="Property value to encrypt / decrypt"
                            .value="${this._text}"
                            @value-changed="${(e) => this._textChanged(e)}" />
                </div>
                <div>
                    <vaadin-button 
                            @click="${this._doAction}"
                            ?disabled="${this._text.trim().length === 0}">
                        Submit
                    </vaadin-button>
                </div>
                ${results}
            </div>
        `;
    }

    _doAction() {
        const successHandler = (jsonRpcResponse) => {
            this._result = jsonRpcResponse.result;
            this._text = '';
        };

        const failureHandler = () => {
            this._result = '';
            notifier.showErrorMessage("Failed "  + this._action + "ing the property value", 'top-start');
        }

        if (this._action === 'encrypt') {
            this.jsonRpc.encryptText({'text': this._text}).then(successHandler, failureHandler);
        } else if (this._action === 'decrypt') {
            this.jsonRpc.decryptText({'text': this._text}).then(successHandler, failureHandler);
        }
    }

    _textChanged(e) {
        this._text = e.detail.value.trim();
        if (this._result.length > 0 && this._text.length > 0) {
            this._result = '';
        }
    }

    _selectAction(e) {
        this._action = e;
        this._text = '';
        this._result = '';
    }

    _copyToClipboard(e) {
        e.stopPropagation();
        const text = this.shadowRoot.querySelector("#result").textContent;
        const listener = function(event) {
            event.clipboardData.setData("text/plain", text);
            event.preventDefault();
        };
        document.addEventListener("copy", listener);
        document.execCommand("copy");
        document.removeEventListener("copy", listener);
        notifier.showInfoMessage("Text copied successfully", 'top-start');
    }
}

customElements.define('qwc-camel-jasypt-utils', QwcCamelJasyptUtils);
