/* chart.js */

// Fake scripture data for testing D3 before connecting PAL data.
const data = Array.from(document.querySelectorAll("#chartDataTable tbody tr"))
.map (function(row){
    const cells = row.querySelectorAll("td");
    
    return {
        bookName: cells[0].textContent.trim(),
        usageCount: Number(cells[1].textContent.trim())
    };
});

console.log(data);

const width = 900;
const height = 450;
const marginTop = 30;
const marginRight = 30;
const marginBottom = 120;
const marginLeft = 70;

d3.select("#chart").selectAll("*").remove();

const svg = d3.select("#chart")
    .append("svg")
    .attr("viewBox", [0, 0, width, height]);

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

svg.append("g")
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
    .attr("width", x.bandwidth());

svg.append("g")
    .attr("transform", "translate(0," + (height - marginBottom) + ")")
    .call(d3.axisBottom(x))
    .selectAll("text")
    .attr("transform", "rotate(-45)")
    .style("text-anchor", "end");

svg.append("g")
    .attr("transform", "translate(" + marginLeft + ",0)")
    .call(d3.axisLeft(y));

svg.append("text")
    .attr("x", marginLeft)
    .attr("y", 18)
    .attr("font-weight", "bold")
    .text("Word usage count");