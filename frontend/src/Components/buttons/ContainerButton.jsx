import React from 'react';
import { getContainer } from '../../API/restapi';

const ContainerButton = ({ sessionID, language }) => {
    return (
        <button onClick={() => getContainer(sessionID, language).then(response => {
            console.log(response);
        })}>
            Get Container
        </button>
    );
};

export default ContainerButton;
