"""
This module provides functions for use with TTS.
"""
from core.actions import Voice
from core.utils import getItemValue
from configuration import sonos, customItemNames
from core.jsr223 import scope
from core.log import logging, LOG_PREFIX

PRIO = {'LOW': 0, 'MODERATE': 1, 'HIGH': 2, 'EMERGENCY': 3}
TIMEOFDAY = {'NIGHT': 0, 'MORNING': 1, 'DAY': 2, 'EVENING': 3} # Regardless of the sun

def tts(ttsSay, ttsPrio=PRIO['MODERATE'], **keywords):
    '''
    Text To Speak function. First argument is positional and mandatory.
    Remaining arguments are optionally keyword arguments.

    Examples:
        .. code-block::

            tts("Hello")
            tts("Hello", PRIO['HIGH'], ttsRoom='Kitchen', ttsVol=42, ttsLang='en-GB', ttsVoice='Brian')

    Args:
        ttsSay (str): text to speak
        ttsPrio (str): (optional) priority as defined by PRIO (defaults to
            PRIO['MODERATE'])
        **keywords: ``ttsRoom`` (room to speak in), ``ttsVol`` (volume),
            ``ttsLang`` (language), ``ttsVoice`` (voice), ``ttsEngine``
            (engine)

    Returns:
        bool: ``True``, if sound was sent, else ``False``
    '''
    log = logging.getLogger(LOG_PREFIX + ".community.sonos.speak")

    def getDefaultRoom():
        # Search for the default room to speak in
        for the_key, the_value in sonos['rooms'].iteritems():
            if the_value['defaultttsdevice']:
                return the_key
        return 'All'

    if getItemValue(customItemNames['allowTTSSwitch'], scope.ON) != scope.ON and ttsPrio <= PRIO['MODERATE']:
        log.info(u"[{}] is OFF and ttsPrio is too low to speak [{}] at this moment".format(customItemNames['allowTTSSwitch'].decode('utf8'), ttsSay))
        return False

    ttsRoom = getDefaultRoom() if 'ttsRoom' not in keywords else keywords['ttsRoom']

    ttsRooms = []
    if ttsRoom == 'All' or ttsRoom is None:
        for the_key, the_value in sonos['rooms'].iteritems():
            ttsRooms.append(sonos['rooms'][the_key])
            log.debug(u"TTS room found: [{}]".format(sonos['rooms'][the_key]['name'].decode('utf8')))
    else:
        sonosSpeaker = sonos['rooms'].get(ttsRoom, None)
        if sonosSpeaker is None:
            log.warn(u"Room [{}] wasn't found in the sonos rooms dictionary".format(ttsRoom.decode('utf8')))
            return
        ttsRooms.append(sonosSpeaker)
        log.debug(u"TTS room found: [{}]".format(sonosSpeaker['name'].decode('utf8')))

    for room in ttsRooms:
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
                ttsVol = room['ttsvolume']

        ttsLang = room['ttslang'] if 'ttsLang' not in keywords else keywords['ttsLang']
        ttsVoice = room['ttsvoice'] if 'ttsVoice' not in keywords else keywords['ttsVoice']
        ttsEngine = room['ttsengine'] if 'ttsEngine' not in keywords else keywords['ttsEngine']
        #Voice.say(ttsSay, "{}:{}".format(ttsEngine, ttsVoice), room['audiosink'])
        Voice.say(ttsSay, "{}:{}".format(ttsEngine, ttsVoice), room['audiosink'], scope.PercentType(ttsVol)) # Volume is not well implemented
        log.info(u"TTS: Speaking [{}] in room [{}] at volume [{}]".format(ttsSay, room['name'].decode('utf8'), ttsVol))

    return True

def greeting():
    """
    To use this, you should set up astro.py as described `here <https://github.com/OH-Jython-Scripters/Script%20Examples/astro.py>`_
    It will take care of updating the item ``V_TimeOfDay`` for you. You can
    customize and/or translate these greetings in your configuration file.
    """
    timeOfDay = getItemValue('V_TimeOfDay', TIMEOFDAY['DAY'])
    try:
       from configuration import timeofdayGreetings
    except ImportError:
        # No customized greetings found in configuration file. We use the following english greetings then
        timeofdayGreetings = {
            0: 'Good night',
            1: 'Good morning',
            2: 'Good day',
            3: 'Good evening'
        }
    if timeOfDay in timeofdayGreetings:
        return timeofdayGreetings[timeOfDay]
    else:
        return 'good day'
