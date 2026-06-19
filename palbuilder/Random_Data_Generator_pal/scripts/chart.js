/* chart */
// ------------------------------------------------------------
    // SAMPLE DATA
    // Later, this can be replaced with data loaded from a CSV/API.
    // Format:
    // { year: 1973, format: "LP/EP", revenue: 8500000000 }
    // ------------------------------------------------------------
    const data = [
      { year: 1973, format: "LP/EP", revenue: 8200000000 },
      { year: 1973, format: "8-Track", revenue: 2400000000 },
      { year: 1973, format: "Cassette", revenue: 600000000 },

      { year: 1980, format: "LP/EP", revenue: 6500000000 },
      { year: 1980, format: "8-Track", revenue: 1200000000 },
      { year: 1980, format: "Cassette", revenue: 3200000000 },

      { year: 1987, format: "LP/EP", revenue: 1800000000 },
      { year: 1987, format: "Cassette", revenue: 5500000000 },
      { year: 1987, format: "CD", revenue: 4200000000 },

      { year: 1995, format: "Cassette", revenue: 1800000000 },
      { year: 1995, format: "CD", revenue: 12500000000 },

      { year: 2003, format: "CD", revenue: 11200000000 },
      { year: 2003, format: "Download", revenue: 500000000 },

      { year: 2010, format: "CD", revenue: 4200000000 },
      { year: 2010, format: "Download", revenue: 3000000000 },
      { year: 2010, format: "Streaming", revenue: 800000000 },

      { year: 2018, format: "CD", revenue: 700000000 },
      { year: 2018, format: "Download", revenue: 1100000000 },
      { year: 2018, format: "Streaming", revenue: 7600000000 }
    ];

    const formats = ["LP/EP", "8-Track", "Cassette", "CD", "Download", "Streaming"];

    const colors = new Map([
      ["LP/EP", "#2A5784"],
      ["8-Track", "#5B8DB8"],
      ["Cassette", "#7AAAD0"],
      ["CD", "#EE7423"],
      ["Download", "#7C4D79"],
      ["Streaming", "#398949"]
    ]);

    const width = 928;
    const height = 520;
    const marginTop = 30;
    const marginRight = 30;
    const marginBottom = 45;
    const marginLeft = 60;

    const tooltip = d3.select("#tooltip");

    // Group data into one object per year:
    // { year: 1973, "LP/EP": 8200000000, "8-Track": 2400000000, ... }
    const years = Array.from(new Set(data.map(d => d.year))).sort((a, b) => a - b);

    const wideData = years.map(year => {
      const row = { year };
      formats.forEach(format => row[format] = 0);

      data
        .filter(d => d.year === year)
        .forEach(d => {
          row[d.format] = d.revenue;
        });

      return row;
    });

    const stack = d3.stack()
      .keys(formats)
      .order(d3.stackOrderReverse);

    const series = stack(wideData);

    const x = d3.scaleBand()
      .domain(years)
      .range([marginLeft, width - marginRight])
      .paddingInner(0.08);

    const y = d3.scaleLinear()
      .domain([0, d3.max(series, layer => d3.max(layer, d => d[1]))])
      .nice()
      .range([height - marginBottom, marginTop]);

    const color = d3.scaleOrdinal()
      .domain(formats)
      .range(formats.map(format => colors.get(format)));

    const svg = d3.select("#chart")
      .append("svg")
      .attr("viewBox", [0, 0, width, height])
      .attr("role", "img")
      .attr("aria-label", "Stacked bar chart showing revenue by music format over time.");

    const formatRevenue = value => {
      if (value >= 1_000_000_000) return "$" + (value / 1_000_000_000).toFixed(1) + "B";
      if (value >= 1_000_000) return "$" + (value / 1_000_000).toFixed(0) + "M";
      return "$" + value.toLocaleString();
    };

    // Draw stacked bars
    svg.append("g")
      .selectAll("g")
      .data(series)
      .join("g")
      .attr("fill", d => color(d.key))
      .selectAll("rect")
      .data(d => d.map(point => ({ ...point, key: d.key })))
      .join("rect")
      .attr("x", d => x(d.data.year))
      .attr("y", d => y(d[1]))
      .attr("height", d => y(d[0]) - y(d[1]))
      .attr("width", x.bandwidth())
      .on("mouseenter", function(event, d) {
        d3.select(this).attr("opacity", 0.78);

        tooltip
          .style("opacity", 1)
          .html(`
            <strong>${d.key}</strong><br>
            Year: ${d.data.year}<br>
            Revenue: ${formatRevenue(d.data[d.key])}
          `);
      })
      .on("mousemove", function(event) {
        tooltip
          .style("left", event.pageX + 14 + "px")
          .style("top", event.pageY - 28 + "px");
      })
      .on("mouseleave", function() {
        d3.select(this).attr("opacity", 1);
        tooltip.style("opacity", 0);
      });

    // X axis
    svg.append("g")
      .attr("class", "axis")
      .attr("transform", `translate(0,${height - marginBottom})`)
      .call(
        d3.axisBottom(x)
          .tickValues(years)
          .tickSizeOuter(0)
      );

    // Y axis
    svg.append("g")
      .attr("class", "axis")
      .attr("transform", `translate(${marginLeft},0)`)
      .call(
        d3.axisLeft(y)
          .ticks(6)
          .tickFormat(d => "$" + d / 1_000_000_000 + "B")
      )
      .call(g => g.select(".domain").remove());

    // Y axis label
    svg.append("text")
      .attr("x", marginLeft)
      .attr("y", 16)
      .attr("fill", "#333")
      .attr("font-size", 13)
      .attr("font-weight", "bold")
      .text("Revenue");

    // Legend
    const legend = d3.select("#legend");

    legend.selectAll(".legend-item")
      .data(formats)
      .join("div")
      .attr("class", "legend-item")
      .html(format => `
        <span class="legend-color" style="background:${colors.get(format)}"></span>
        <span>${format}</span>
      `);