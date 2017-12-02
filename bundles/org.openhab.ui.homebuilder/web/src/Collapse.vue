<template>
    <div class="pb-1">
        <b-btn block v-b-toggle="uid" variant="info" v-html="heading"></b-btn>
        <b-collapse :id="uid" accordion="preview">
            <b-card>
                <button @click="createItems" v-if="uid === 'items' && content instanceof Array" class="btn-clipboard btn-create-items">{{ createText }}</button>
                <button @click="copy" class="btn-clipboard" title="Copy to clipboard">{{ copyText }}</button>

                <span class="pb-1" v-if="uid === 'items' && content instanceof Array">
                    Number of Items: {{content.length}}
                </span>

                <span class="pb-1" v-if="uid === 'habpanel'">
                    Paste the content in
                    <a href="../../habpanel/index.html#/settings/localconfig" target="_blank">this config page</a>.
                </span>
                <pre v-html='content'></pre>
            </b-card>
        </b-collapse>
    </div>
</template>

<script>
import Vue from 'vue'
import Clipboard from 'v-clipboard'

Vue.use(Clipboard);

export default {
    props: ['uid', 'heading', 'content'],
    data: () => ({
        copyText: 'Copy',
        createText: 'Create Items'
    }),
    http: {
        root: window.location.origin + '/rest/'
    },
    methods: {
        copy() {
            let content = this.content;

            if (typeof content === 'object') {
                content = JSON.stringify(this.content, null, 2);
            } else {
                content = content
                    .replace(/&lt;/g, '<')
                    .replace(/&gt;/g, '>');
            }

            this.$clipboard(content);

            this.copyText = 'Copied!';

            setTimeout(() => {
                this.copyText = 'Copy';
            }, 500);
        },
        createItems() {
            let content = this.content;
            if (typeof content === 'object') {
                this.$http
                    .put('items', content)
                    .then(response => {
                        if (response.status === 200) {
                            this.createText = 'Done!';
                        } else {
                            this.createText = 'Error!';
                        }

                        setTimeout(() => {
                            this.createText = 'Create Items';
                        }, 500);
                    });
            }
        }
    }
}
</script>
