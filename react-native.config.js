const path = require('path');

module.exports = {
  dependency: {
    platforms: {
      ios: {},
      android: {
      	packageImportPath: 'import com.xbenjii.RNBraintreeDropIn.RNBraintreeDropInPackage;',
        packageInstance: 'new RNBraintreeDropInPackage()',
      },
    },
  },
};
