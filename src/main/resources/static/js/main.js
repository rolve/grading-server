/**
 * @param time {Date}
 * @param lang {string}
 * @returns {string}
 */
function relativeTime(time, lang) {
    let secondsDiff = Math.round((time - Date.now()) / 1000);
    let unitsInSec = [60, 3600, 86400, 86400 * 7, 86400 * 30, 86400 * 365, Infinity];
    let unitStrings = ["second", "minute", "hour", "day", "week", "month", "year"];
    let unitIndex = unitsInSec.findIndex((cutoff) => cutoff > Math.abs(secondsDiff));
    let divisor = unitIndex ? unitsInSec[unitIndex - 1] : 1;

    let rtf = new Intl.RelativeTimeFormat(lang, { numeric: "auto" });
    return rtf.format(Math.trunc(secondsDiff / divisor), unitStrings[unitIndex]);
}

/**
 * @param time {Date}
 * @param lang {string}
 * @returns {string}
 */
function absoluteTime(time, lang) {
    let dtf = new Intl.DateTimeFormat(lang, {
        weekday: "long",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "numeric",
        minute: "numeric",
        timeZoneName: "short"
    });
    return dtf.format(time);
}

function onLoadListener() {
    let lang = document.documentElement.lang;
    let times = document.querySelectorAll("time[data-zoned-date-time]");
    for (let timeElem of times) {
        let time = new Date(timeElem.getAttribute("datetime"));
        timeElem.textContent = relativeTime(time, lang);
        let tooltipElem = timeElem.nextElementSibling;
        tooltipElem.textContent = absoluteTime(time, lang);
    }
}

window.addEventListener("turbo:load", onLoadListener);
window.addEventListener("turbo:render", onLoadListener);
