<#if includeMetaData!>
	<div class="form-cell form-group <#if error??>has-error</#if>" ${elementMetaData!}>
	    <label>${element.properties.label} <span class="form-cell-validator">${decoration}</span></label>
	    <#if (element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') >
	        <div class="form-cell-value"><span>${value!?html}</span></div>
	        <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" class="form-control" />
	    <#else>
	        <input id="${elementParamName!}" name="${elementParamName!}" type="password" class="form-control" size="${element.properties.size!}" value="${value!?html}" maxlength="${element.properties.maxlength!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.readonly! == 'true'>readonly</#if> />
	    	<span id="${elementParamName!}-generate-otp" name="${elementParamName!}-generate-otp" class="otp-button">
	    		${element.properties.generateTokenButtonLabel!}
	    	</span>
	    </#if>
	    <#if error??> <span class="form-error-message help-block">${error}</span></#if>
	</div>
<#elseif (element.properties.readonly! != 'true')>
    <#assign elementId = elementParamName + element.properties.elementUniqueKey>
	<div class="form-cell form-group <#if error??>has-error</#if>" ${elementMetaData!}>
		<label class="control-label">${element.properties.label} <span class="form-cell-validator">${decoration}</span></label>
		<div class="input-group">
            <input id="${elementId!}" name="${elementParamName!}" type="password" class="form-control" placeholder="OTP" value="${value!?html}" maxlength="${element.properties.maxlength!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.readonly! == 'true'>readonly</#if> />
            <span class="input-group-btn">
                <button type="button" class="btn btn-primary btn-sm" id="${elementId!}-generate-otp" name="${elementParamName!}-generate-otp">${element.properties.generateTokenButtonLabel!}</button>
            </span>
       </div>
       <img id="${elementId!}-loading" src="${request.contextPath}/plugin/${className}/images/spin.gif" height="24" width="24" style="vertical-align: middle; display: none;">
		<#if error??> <span class="form-error-message help-block">${error}</span></#if>
	</div>
	<script>
		$("#${elementId!}-generate-otp").on("click",function(){
		    $("#${elementId!}-loading").show();
		    let $errorMessage = $("#${elementId!}-loading").parent().find("span.form-error-message.help-block");
            if($errorMessage) {
                $errorMessage.hide();
            }

			var jsonData = {
	            formId : "${FORM_ID!}",
	            sectionId : "${SECTION_ID!}",
	            fieldId : "${FIELD_ID!}",
	            primaryKey : "${PRIMARY_KEY!}",
                processId : "${PROCESS_ID!}",
                activityId : "${ACTIVITY_ID!}",
	            username : "${USERNAME!}",
	            nonce : "${NONCE!}"
	        };

			$.ajax({
				url: "${request.contextPath}/web/json/app/${appId!}/${appVersion!}/plugin/${className}/service?${requestParameter}",
				type : 'POST',
				dataType: 'json',
	            headers : { 'Content-Type' : 'application/json' },
	            data : JSON.stringify(jsonData)
			})
			.always(function(data) {
                $("#${elementId!}-loading").hide();
                $("#${elementId!}").val("");
                $("#${elementId!}-loading").parent().removeClass("has-error");
                $("#${elementId!}-loading").parent().find("span.form-error-message.help-block").remove();
                $("#${elementId!}-loading").parent().append('<span class="form-error-message help-block">'+data.message+'</span>');
            })
            .fail(function(data) {
                let response = JSON.parse(data.responseText);
                $("#${elementId!}-loading").parent().addClass("has-error");
                $("#${elementId!}-loading").parent().find("span.form-error-message.help-block").remove();
                $("#${elementId!}-loading").parent().append('<span class="form-error-message help-block">'+response.error.message+'</span>');
            });
		});
	</script>
</#if>
