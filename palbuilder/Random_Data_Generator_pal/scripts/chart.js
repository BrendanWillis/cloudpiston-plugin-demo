/* chart.js */

const meta = document.getElementById("chartMeta");

const searchWord = meta ? meta.getAttribute("data-word") : "";
const searchMode = meta ? meta.getAttribute("data-mode") : "verses";
const searchModeLabel = meta ? meta.getAttribute("data-mode-label") : "Verses only";

let yAxisLabel = "Verse occurrences";

if (searchMode === "summaries") {
    yAxisLabel = "Summary occurrences";
} else if (searchMode === "both") {
    yAxisLabel = "Combined occurrences";
}

const data = Array.from(document.querySelectorAll("#chartDataTable tbody tr"))
    .map(function(row) {
        const cells = row.querySelectorAll("td");

        return {
            bookName: cells[0].textContent.trim(),
            usageCount: Number(cells[1].textContent.trim())
        };
    });

console.log(data);

const width = 1000;
const height = 520;
const marginTop = 50;
const marginRight = 30;
const marginBottom = 170;
const marginLeft = 80;

d3.select("#chart").selectAll("*").remove();

const svg = d3.select("#chart")
    .append("svg")
    .attr("viewBox", [0, 0, width, height]);
// Tooltip shown when the mouse hovers over a bar.
const tooltip = d3.select("#tooltip");

function getBookGroup(bookName) {
    const newTestament = [
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans",
        "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
        "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    ];

    const bookOfMormon = [
        "Book of Mormon Title Page", "1 Nephi", "2 Nephi", "Jacob",
        "Enos", "Jarom", "Omni", "Words of Mormon", "Mosiah", "Alma",
        "Helaman", "3 Nephi", "4 Nephi", "Mormon", "Ether", "Moroni"
    ];

    const dc = [
        "Doctrine and Covenants", "Official Declaration"
    ];

    const pgp = [
        "Pearl of Great Price", "Moses", "Abraham",
        "Joseph Smith—Matthew", "Articles of Faith"
    ];

    if (newTestament.indexOf(bookName) >= 0) return "New Testament";
    if (bookOfMormon.indexOf(bookName) >= 0) return "Book of Mormon";
    if (dc.indexOf(bookName) >= 0) return "Doctrine and Covenants";
    if (pgp.indexOf(bookName) >= 0) return "Pearl of Great Price";

    return "Old Testament";
}

const color = d3.scaleOrdinal()
    .domain([
        "Old Testament",
        "New Testament",
        "Book of Mormon",
        "Doctrine and Covenants",
        "Pearl of Great Price"
    ])
    .range([
        "#4e79a7",
        "#f28e2b",
        "#59a14f",
        "#e15759",
        "#b07aa1"
    ]);

const x = d3.scaleBand()
    .domain(data.map(function(d) {
        return d.bookName;
    }))
    .range([marginLeft, width - marginRight])
    .padding(0.2);

const y = d3.scaleLinear()
    .domain([0, d3.max(data, function(d) {
        return d.usageCount;
    })])
    .nice()
    .range([height - marginBottom, marginTop]);

svg.append("text")
    .attr("x", marginLeft)
    .attr("y", 24)
    .attr("font-weight", "bold")
    .attr("font-size", "20px")
    .text(yAxisLabel);

svg.append("text")
    .attr("x", marginLeft)
    .attr("y", 44)
    .attr("font-size", "12px")
    .attr("fill", "#666")
    .text('Search: "' + searchWord + '" — ' + searchModeLabel);

const bars = svg.append("g")
    .selectAll("rect")
    .data(data)
    .join("rect")
    .attr("x", function(d) {
        return x(d.bookName);
    })
    .attr("y", function(d) {
        return y(d.usageCount);
    })
    .attr("height", function(d) {
        return y(0) - y(d.usageCount);
    })
    .attr("width", x.bandwidth())
    .attr("fill", function(d) {
        return color(getBookGroup(d.bookName));
    })

    .on("mouseover", function(event, d) {

    d3.select(this)
        .attr("stroke", "#222")
        .attr("stroke-width", 2)
        .attr("opacity", 0.85);

    tooltip
        .style("opacity", 1)
        .style("display", "block")
        .html(
            "<strong>" + d.bookName + "</strong><br/>" +
            yAxisLabel + ": <strong>" + d.usageCount + "</strong>"
        );
})

    .on("mousemove", function(event) {
    
        tooltip
            .style("left", (event.pageX + 15) + "px")
            .style("top", (event.pageY - 30) + "px");
    })
    
    .on("mouseout", function() {
    
        d3.select(this)
            .attr("stroke", null)
            .attr("stroke-width", null)
            .attr("opacity", 1);
    
        tooltip
            .style("opacity", 0)
            .style("display", "none");
    });

    bars.append("title")
        .text(function(d) {
            return d.bookName + "\n" + yAxisLabel + ": " + d.usageCount;
        });

svg.append("g")
    .attr("transform", "translate(0," + (height - marginBottom) + ")")
    .call(d3.axisBottom(x))
    .selectAll("text")
    .attr("transform", "rotate(-60)")
    .style("text-anchor", "end")
    .style("font-size", "11px");

svg.append("g")
    .attr("transform", "translate(" + marginLeft + ",0)")
    .call(d3.axisLeft(y));

svg.append("text")
    .attr("transform", "rotate(-90)")
    .attr("x", -((height - marginBottom + marginTop) / 2))
    .attr("y", 22)
    .attr("text-anchor", "middle")
    .attr("font-weight", "bold")
    .text(yAxisLabel);

svg.append("text")
    .attr("x", (width / 2))
    .attr("y", height - 20)
    .attr("text-anchor", "middle")
    .attr("font-weight", "bold")
    .text("Book of Scripture");

const legendData = color.domain();

const legend = d3.select("#legend");
legend.selectAll("*").remove();

legend.selectAll(".legend-item")
    .data(legendData)
    .join("div")
    .attr("class", "legend-item")
    .html(function(d) {
        return '<span class="legend-color" style="background-color:' + color(d) + '"></span>' + d;
    });