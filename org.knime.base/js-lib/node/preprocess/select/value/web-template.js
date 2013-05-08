// JavaScript Document
var select_value = {};
var knime_select_value = {};
knime_select_value.init = function (viewContent, container) {
	console.log("select_value.init called");
	knime_select_value.data = JSON.parse(viewContent);
	var table = document.createElement("table");
	table.setAttribute("id", "selectionTable");
	document.getElementById(container).appendChild(table);
	for (var i = 0; i < knime_select_value.data.possibleValues.length; i++) {
		var checkBoxField = document.createElement("td");
		var checkBox = document.createElement("input");
		checkBox.setAttribute("type", "checkbox");
		checkBox.setAttribute("name", knime_select_value.data.possibleValues[i]);
		
		if (knime_select_value.data.selectedValues != "null" && knime_select_value.data.selectedValues[i] == knime_select_value.data.possibleValues[i]) {
			checkBox.setAttribute("checked", true);
		}
		checkBoxField.appendChild(checkBox);
		var textField = document.createElement("td");
		textField.innerHTML = knime_select_value.data.possibleValues[i];
		var row = document.createElement("tr");
		row.appendChild(checkBoxField);
		row.appendChild(textField);
		table.appendChild(row);
	}
}

knime_select_value.pullViewContent = function(container) {
	console.log("select_value.pullViewContent called")
	knime_select_value.data.selectedValues = [];
	var checkBoxes = document.getElementById(container).getElementsByTagName("input");
	for (var i = 0; i < checkBoxes.length; i++) {
		if (checkBoxes[i].checked) {
			console.log("pushing input, value: " + checkBoxes[i].getAttribute("name"))
			knime_select_value.data.selectedValues.push(checkBoxes[i].getAttribute("name"));
		}
	}
	var returnString = JSON.stringify(knime_select_value.data);
	console.log(returnString);
	return returnString;
}