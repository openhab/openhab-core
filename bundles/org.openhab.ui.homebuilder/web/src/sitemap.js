import * as _ from 'lodash'
import * as s from 'underscore.string'
import { floors } from './definitions'
import { getChosenObjects, getItems, GROUP_PREFIX } from './restItems'

export let sitemapName = '';

export function generateSitemap(model) {
    let lines = [];
    let chosenObjects = getChosenObjects(model);
    const items = getItems(model);

    sitemapName = s(model.homeName)
        .slugify()
        .value()
        .replace(/-/g, '_');

    lines.push(
        'sitemap ' + sitemapName + ' label="' + model.homeName + '" {'
    );

    for (let i = 0; i < model.floorsCount; i++) {
        let floor = floors[i];
        let floorPrefix = '';

        if (model.floorsCount > 1) {
            let icon = model.itemsIcons && floor.icon ? '" icon="' + floor.icon : '';
            lines.push(s.pad(' ', 4) + 'Frame label="' + (floor.name || floor.value) + icon + '" {');
            floorPrefix = floor.abbr + '_';
        } else {
            lines.push(s.pad(' ', 4) + 'Frame {');
        }

        if (floor && floor.value && !_.isUndefined(model[floor.value])) {
            model[floor.value].forEach(function(room) {
                lines.push(s.pad(' ', 8) + 'Group item=' + floorPrefix + room.value);
            });
        }
        lines.push(s.pad(' ', 4) + '}');

        if (i < model.floorsCount) {
            lines.push('');
        }
    }

    if (chosenObjects.length) {
        // Objects node
        lines.push(s.pad(' ', 4) + 'Frame {');

        chosenObjects.forEach(function(object, index, collection) {
            let groupName = GROUP_PREFIX + object;
            let group = _.find(items, { name: groupName });
            let groupItems = _.filter(items, function(item) {
                return item.groupNames && item.groupNames.includes(groupName)
            });
            let textGroup = `Text label="${group.label}"`;
            textGroup += group.category && group.category !== 'none' ? ` icon="${group.category}" {` : ` {`;

            lines.push(s.pad(' ', 8) + textGroup);

            groupItems.forEach((item) => {
                let room = _.find(items, { name: item.groupNames[0] });
                lines.push(
                    s.pad(' ', 12) + `Default item=${item.name} label="${room.label}"`
                );
            });

            // End of "Text" node
            lines.push(s.pad(' ', 8) + '}');

            if (index < collection.length - 1) {
                lines.push('');
            }
        });
        lines.push(s.pad(' ', 4) + '}');
    }

    lines.push('}');

    return lines.join('\n');
}

