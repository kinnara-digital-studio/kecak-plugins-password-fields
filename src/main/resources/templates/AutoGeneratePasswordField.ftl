<div class="form-cell" ${elementMetaData!}>
    <#assign elementId = elementParamName + element.properties.elementUniqueKey>

    <label class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
    <#if (element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') >
        <span>*************</span>
        <input id="${elementId!}" name="${elementParamName!}" type="hidden" value="${value!?html}" />
    <#else>
        <input id="${elementId!}" name="${elementParamName!}" type="password" size="${element.properties.size!}" value="${value!?html}" maxlength="${element.properties.maxlength!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.readonly! == 'true'>readonly</#if> />
        <input type="button" style="min-height:0px; line-height:20px;" id="${elementId!}-generate-password" name="${elementParamName!}-generate-otp" class="form-button" value="${element.properties.generateTokenButtonLabel!}" />
        <img id="${elementId!}-loading" src="${request.contextPath}/plugin/${className!}/images/spin.gif" height="24" width="24" style="vertical-align: middle; display: none;">
    </#if>

    <script>
        $("#${elementId!}-generate-password").click(function(){
            $("#${elementId!}-loading").show();
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
                url: "${request.contextPath}/web/json/app/${appId!}/${appVersion!}/plugin/${className}/service?${requestParameter!}",
                type : 'POST',
                dataType: 'json',
                headers : { 'Content-Type' : 'application/json' },
                data : JSON.stringify(jsonData),
            })
            .always(function(data) {
                $("#${elementId!}-loading").hide();
                $("#${elementId!}").val(data.randomPassword);
                $("#${elementId!}").parent().find("label").find("span.form-error-message").remove();
                $("#${elementId!}").parent().find("label").append('<span class="form-error-message">'+data.message+'</span>');
            })
            .fail(function(data) {
                let response = JSON.parse(data.responseText);
                $("#${elementId!}").parent().find("label").find("span.form-error-message").remove();
                $("#${elementId!}").parent().find("label").append('<span class="form-error-message">'+response.error.message+'</span>');
                $("#${elementId!}").val("");
            });
        });
    </script>
</div>