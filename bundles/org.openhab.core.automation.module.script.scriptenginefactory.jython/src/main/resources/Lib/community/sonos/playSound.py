"""
This module provides functions for playing sounds.
"""
from core.actions import Audio
from core.utils import getItemValue
from configuration import sonos, customItemNames
from core.jsr223 import scope
from core.log import logging, LOG_PREFIX

PRIO = {'LOW': 0, 'MODERATE': 1, 'HIGH': 2, 'EMERGENCY': 3}
TIMEOFDAY = {'NIGHT': 0, 'MORNING': 1, 'DAY': 2, 'EVENING': 3} # Regardless of the sun

def playsound(fileName, ttsPrio=PRIO['MODERATE'], **keywords):
    """
    Play a sound mp3 file function. First argument is positional and mandatory.
    Remaining arguments are optionally keyword arguments.

    Examples:
        .. code-block::

            playsound("Hello.mp3")
            playsound("Hello.mp3", PRIO['HIGH'], room='Kitchen', volume=42)

    Args:
        fileName (str): Sound file name to play (files need to be put in the
            folder ``/conf/sounds/``)
        ttsPrio (str): (optional) priority as defined by PRIO (defaults to
            PRIO['MODERATE'])
        **keywords: ``room`` (room to play in defaults to ``All``) and
            ``ttsVol`` (volume)

    Returns:
        bool: ``True``, if sound was sent, else ``False``
    """
    log = logging.getLogger(LOG_PREFIX + ".community.sonos.playsound")

    def getDefaultRoom():
        # Search for the default room to speak in
        for the_key, the_value in sonos['rooms'].iteritems():
            if the_value['defaultttsdevice']:
                return the_key
        return 'All'

    if getItemValue(customItemNames['allowTTSSwitch'], scope.ON) != scope.ON and ttsPrio <= PRIO['MODERATE']:
        log.info("[{}] is OFF and ttsPrio is too low to play the sound [{}] at this moment".format(customItemNames['allowTTSSwitch'], fileName))
        return False

    room = getDefaultRoom() if 'room' not in keywords else keywords['room']

    rooms = []
    if room == 'All' or room is None:
        for the_key, the_value in sonos['rooms'].iteritems():
            rooms.append(sonos['rooms'][the_key])
            log.debug(u"Room found: [{}]".format(sonos['rooms'][the_key]['name'].decode('utf8')))
    else:
        sonosSpeaker = sonos['rooms'].get(room, None)
        if sonosSpeaker is None:
            log.warn(u"Room [{}] wasn't found in the sonos rooms dictionary".format(room.decode('utf8')))
            return
        rooms.append(sonosSpeaker)
        log.debug(u"Room found: [{}]".format(sonosSpeaker['name'].decode('utf8')))

    for aRoom in rooms:
        ttsVol = None if 'ttsVol' not in keywords else keywords['ttsVol']
        if not ttsVol or ttsVol >= 70:
            if ttsPrio == PRIO['LOW']:
                ttsVol = 30
            elif ttsPrio == PRIO['MODERATE']:
                ttsVol = 40
            elif ttsPrio == PRIO['HIGH']:
                ttsVol = 60
            elif ttsPrio == PRIO['EMERGENCY']:
                ttsVol = 70
            else:
                ttsVol = aRoom['ttsvolume']

        Audio.playSound(aRoom['audiosink'], fileName)
        log.info(u"playSound: Playing [{}] in room [{}] at volume [{}]".format(fileName.decode('utf8'), aRoom['name'].decode('utf8'), ttsVol))

    return True
