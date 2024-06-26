<#-- $This file is distributed under the terms of the license in LICENSE$ -->
<#assign visContainerID = '${sparklineVO.visContainerDivID}'>

<#if sparklineVO.shortVisMode>
    <#assign sparklineContainerID = 'unique_coinvestigators_short_sparkline_vis'>
<#else>
    <#assign sparklineContainerID = 'unique_coinvestigators_full_sparkline_vis'>
</#if>

<#-- This is used to prevent collision between sparkline & visualization conatiner div ids. -->
<#if visContainerID?upper_case == sparklineContainerID?upper_case>
    <#assign sparklineContainerID = visContainerID + "_spark">
</#if>

<div class="staticPageBackground">
    <div id="${visContainerID}">
        <script type="text/javascript">
            <#if sparklineVO.shortVisMode>
                var visualizationOptions = {
                    width: 150,
                    height: 60,
                    color: '3399CC',
                    chartType: 'ls',
                    chartLabel: 'r'
                };
            <#else>
                var visualizationOptions = {
                    width: 250,
                    height: 75,
                    color: '3399CC',
                    chartType: 'ls',
                    chartLabel: 'r'
                };
            </#if>

            function drawCoInvestigatorsSparklineVisualization(providedSparklineImgTD) {

                var unknownYearGrantCounts = ${sparklineVO.unknownYearGrants};
                var onlyUnknownYearGrants = false;

                var data = new google.visualization.DataTable();
                data.addColumn('number', '${i18n().year_capitalized}');
                data.addColumn('number', '${i18n().unique_coinvestigators}');
                data.addRows(${sparklineVO.yearToEntityCountDataTable?size});

                var knownYearGrantCounts = 0;

                <#list sparklineVO.yearToEntityCountDataTable as yearToUniqueCoinvestigatorsDataElement>
                    data.setValue(${yearToUniqueCoinvestigatorsDataElement.yearToEntityCounter}, 0, ${yearToUniqueCoinvestigatorsDataElement.year});
                    data.setFormattedValue(${yearToUniqueCoinvestigatorsDataElement.yearToEntityCounter}, 0, '${yearToUniqueCoinvestigatorsDataElement.year}');
                    data.setValue(${yearToUniqueCoinvestigatorsDataElement.yearToEntityCounter}, 1, ${yearToUniqueCoinvestigatorsDataElement.currentEntitiesCount});
                    knownYearGrantCounts += ${yearToUniqueCoinvestigatorsDataElement.currentEntitiesCount};
                </#list>

                <#-- Create a view of the data containing only the column pertaining to coinvestigators count. -->
                var sparklineDataView = new google.visualization.DataView(data);

                <#if sparklineVO.shortVisMode>

                <#-- For the short view we only want the last 10 year's view of coinvestigators count, hence we filter
                    the data we actually want to use for render. -->

                sparklineDataView.setRows(data.getFilteredRows([{
                        column: 0,
                        minValue: '${sparklineVO.earliestRenderedGrantYear?c}',
                        maxValue: '${sparklineVO.latestRenderedGrantYear?c}'
                }]));

                <#else>

                </#if>

                /*
                This means that all the publications have unknown years & we do not need to display
                the sparkline.
                */
                if (unknownYearGrantCounts > 0 && knownYearGrantCounts < 1) {

                    onlyUnknownYearGrants = true;

                } else {

                    <#-- Create the vis object and draw it in the div pertaining to sparkline. -->
                    var sparkline = new google.visualization.ImageSparkLine(providedSparklineImgTD[0]);
                    sparkline.draw(sparklineDataView, {
                        width: visualizationOptions.width,
                        height: visualizationOptions.height,
                        showAxisLines: false,
                        showValueLabels: false,
                        labelPosition: 'none',
                        legend: { position: 'none' },
                        chartArea: {'width': '100%', 'height': '100%'},
                        colors: ['3399CC'],
                        hAxis: {
                            gridlines: {color: 'transparent'},
                            baselineColor: 'transparent'
                        },
                        vAxis: {
                            gridlines: {color: 'transparent'},
                            baselineColor: 'transparent'
                        },
                        backgroundColor: {
                            stroke: '#cfe4ed',
                            strokeWidth: 2
                        },
                        tooltip: { 
                            textStyle: {fontSize: 14}
                        }
                    });

                }

                if (${sparklineVO.totalCollaborationshipCount?c}) {
                    var totalGrantCount = ${sparklineVO.totalCollaborationshipCount?c};
                } else {
                    var totalGrantCount = knownYearGrantCounts + unknownYearGrantCounts;
                }



                <#if sparklineVO.shortVisMode>

                    <#-- We want to display how many coinvestigators were considered, so this is used to calculate this. -->

                    var shortSparkRows = sparklineDataView.getViewRows();
                    var renderedShortSparks = 0;
                    $.each(shortSparkRows, function(index, value) {
                        renderedShortSparks += data.getValue(value, 1);
                    });

                    /*
                    In case that there are only unknown grants we want the text to mention these counts,
                    which would not be mentioned in the other case because the renderedShortSparks only hold counts
                    of grants which have any date associated with it.
                    */
                    var totalGrants = onlyUnknownYearGrants ? unknownYearGrantCounts : renderedShortSparks;


                    if (totalGrants === 1) {
                        var grantDisplay = "${i18n().co_investigator?js_string}";
                    } else {
                        var grantDisplay = "${i18n().co_investigators?js_string}";
                    }

                    $('#${sparklineContainerID} td.sparkline_number').text(totalGrants).css("font-weight", "bold").attr("class", "grey").append("<span style='color: #2485AE;'> " + grantDisplay + " <br/></span>");

                    var sparksText = '  ${i18n().within_last_10_years?js_string}';

                    if (totalGrants !== totalGrantCount) {
                        sparksText += ' (' + totalGrantCount + ' ${i18n().total?js_string})';
                    }

                 <#else>

                    /*
                     * Sparks that will be rendered will always be the one's which has
                     * any year associated with it. Hence.
                     * */
                    var renderedSparks = ${sparklineVO.renderedSparks};

                    /*
                    In case that there are only unknown grants we want the text to mention these counts,
                    which would not be mentioned in the other case because the renderedSparks only hold counts
                    of grants which have any date associated with it.
                    */
                    var totalGrants = onlyUnknownYearGrants ? unknownYearGrantCounts : renderedSparks;

                    if (totalGrants === 1) {
                        var grantDisplay = "${i18n().co_investigator?js_string}";
                    } else {
                        var grantDisplay = "${i18n().co_investigators?js_string}";
                    }

                    $('#${sparklineContainerID} td.sparkline_number').text(totalGrants).css("font-weight", "bold").attr("class", "grey").append("<span style='color: #2485AE;'> " + grantDisplay + " <br/></span>");

                    var sparksText = '  ${i18n().from?js_string} <span class="sparkline_range">${sparklineVO.earliestYearConsidered?c}'
                                        + ' through ${sparklineVO.latestRenderedGrantYear?c}</span>';

                    if (totalGrants !== totalGrantCount) {
                        sparksText += ' (' + totalGrantCount + ' ${i18n().total?js_string})';
                    }

                    if (totalGrantCount) {
                        sparksText += '<br /> <a href="${sparklineVO.downloadDataLink}" title="csv ${i18n().file_capitalized}">(.CSV ${i18n().file_capitalized})</a> ';
                    }

                 </#if>

                 if (!onlyUnknownYearGrants) {
                    $('#${sparklineContainerID} td.sparkline_text').html(sparksText).css("font-weight", "bold");
                 }

           }

            /*
             * This will activate the visualization. It takes care of creating
             * div elements to hold the actual sparkline image and then calling the
             * drawCoInvestigatorsSparklineVisualization function.
             * */

            $(document).ready(function() {
                var sparklineImgTD;

            /*
             * This is a nuclear option (creating the container in which everything goes)
             * the only reason this will be ever used is the API user never submitted a
             * container ID in which everything goes. The alternative was to let the
             * vis not appear in the calling page at all. So now atleast vis appears but
             * appended at the bottom of the body.
             * */

                if ($('#${visContainerID}').length === 0) {
                    $('<div/>', {
                        'id': '${visContainerID}'
                    }).appendTo('body');
                }


                if ($('#${sparklineContainerID}').length === 0) {

                    $('<div/>', {
                        'id': '${sparklineContainerID}',
                        'class': 'sparkline_style'
                    }).prependTo('#${visContainerID}');

                    var table = $('<table>');
                    table.attr('class', 'sparkline_wrapper_table');
                    var row = $('<tr>');
                    sparklineImgTD = $('<td>');
                    sparklineImgTD.attr('id', '${sparklineContainerID}_img');
                    sparklineImgTD.attr('width', visualizationOptions.width);
                    sparklineImgTD.attr('class', 'sparkline_style');

                    row.append(sparklineImgTD);
                    var row2 = $('<tr>');
                    var sparklineNumberTD = $('<td>');
                    sparklineNumberTD.attr('class', 'sparkline_number');
                    sparklineNumberTD.css('text-align', 'left');
                    row2.append(sparklineNumberTD);
                    var row3 = $('<tr>');

                    var sparklineTextTD = $('<td>');
                    sparklineTextTD.attr('class', 'sparkline_text');
                    sparklineTextTD.css('text-align', 'left');
                    row3.append(sparklineTextTD);
                    table.append(row);
                    table.append(row2);
                    table.append(row3);
                    table.prependTo('#${sparklineContainerID}');

                }
                google.charts.load('current', {
                    callback: function() {
                        drawCoInvestigatorsSparklineVisualization(sparklineImgTD)
                    },
                    packages: ['bar', 'corechart', 'table', 'imagesparkline']
                });
            });
        </script>

    </div>

    <!-- Sparkline Viz -->

    <#if sparklineVO.shortVisMode>
        <#--<span class="vis_link">-->
            <p><a class="all-vivo-grants" href="${sparklineVO.fullTimelineNetworkLink}" title="${i18n().view_timeline_copi_network}">${i18n().view_timeline_copi_network}</a></p>
        <#--</span>-->
    <#else>
        <!-- For Full Sparkline - Print the Table of CoInvestigator Counts per Year -->
            <#if displayTable?? && displayTable>

                <p>
                    <#assign tableID = "coinve_sparkline_data_table" />
                    <#assign tableCaption = "${i18n().unique_coinvestigators_per_year} " />
                    <#assign tableActivityColumnName = "${i18n().count_capitalized}" />
                    <#assign tableContent = sparklineVO.yearToActivityCount />
                    <#assign fileDownloadLink = sparklineVO.downloadDataLink />

                    <#include "yearToActivityCountTable.ftl">

                    ${i18n().download_data_as} <a href="${sparklineVO.downloadDataLink}" title="csv ${i18n().download}">.csv</a> ${i18n().file}.
                    <br />
                </p>

            </#if>
    </#if>
</div>
